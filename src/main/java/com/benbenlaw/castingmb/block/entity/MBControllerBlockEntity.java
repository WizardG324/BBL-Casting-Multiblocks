package com.benbenlaw.castingmb.block.entity;

import com.benbenlaw.casting.block.custom.CastingBlock;
import com.benbenlaw.casting.block.entity.FluidAccepting;
import com.benbenlaw.casting.block.entity.FluidSending;
import com.benbenlaw.casting.item.CastingDataComponents;
import com.benbenlaw.casting.item.FluidMoverItem;
import com.benbenlaw.casting.item.util.FluidListComponent;
import com.benbenlaw.casting.recipe.custom.MeltingRecipe;
import com.benbenlaw.castingmb.block.CastingMBBlockEntities;
import com.benbenlaw.castingmb.block.custom.MBControllerBlock;
import com.benbenlaw.castingmb.block.entity.handler.DynamicInputItemHandler;
import com.benbenlaw.castingmb.block.entity.handler.MultiFluidResourceHandler;
import com.benbenlaw.castingmb.network.packets.SyncFuelTanks;
import com.benbenlaw.castingmb.recipe.EntityMeltingRecipe;
import com.benbenlaw.castingmb.screen.MBControllerMenu;
import com.benbenlaw.castingmb.util.CastingMBTags;
import com.benbenlaw.castingmb.util.MBData;
import com.benbenlaw.core.block.entity.SyncableBlockEntity;
import com.benbenlaw.core.block.entity.handler.fluid.SyncableFluidHandler;
import com.benbenlaw.core.block.entity.handler.item.SyncableItemHandler;
import com.benbenlaw.core.multiblock.CoreMultiblockDetector;
import com.benbenlaw.core.multiblock.MultiblockData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MBControllerBlockEntity extends SyncableBlockEntity implements MenuProvider, FluidSending, FluidAccepting {

    private final ContainerData data;
    private int[] progress = new int[100];
    private int[] maxProgress = new int[100];
    private OptionalInt temperature = OptionalInt.empty();

    public MultiblockData cachedMultiblockData = null;
    public Set<BlockPos> multiblockBlockPos = new HashSet<>();
    private List<BlockPos> cachedTankPositions = List.of();
    public boolean structureDirty = true;

    private final RecipeHolder<MeltingRecipe>[] cachedSlotRecipes = new RecipeHolder[100];
    private final boolean[] slotRecipeDirty = new boolean[100];
    private final Map<EntityType<?>, RecipeHolder<EntityMeltingRecipe>> entityRecipeCache = new HashMap<>();
    private RecipeManager lastRecipeManager = null;



    {
        Arrays.fill(slotRecipeDirty, true);
    }

    private int regulatorCount = 0;
    private int maxItemSlots = 0;
    private BlockPos clientSideFuelTankPos;
    private final Map<UUID, Integer> entityDamageCooldowns = new HashMap<>();

    private final SyncableItemHandler inventory =
            new DynamicInputItemHandler(this, 100, (i, stack) -> i >= 0 && i < 99, i -> i == 100) {
                @Override
                protected void onContentsChanged(int index, ItemStack previousContents) {
                    if (index >= 0 && index < slotRecipeDirty.length) {
                        slotRecipeDirty[index] = true;
                    }
                    super.onContentsChanged(index, previousContents);
                }
            };

    private final MultiFluidResourceHandler fluidInventory =
            new MultiFluidResourceHandler(this, 1, 0,
                    (i, stack) -> i < this.fluidInventory.getMaxFluidTypes(),
                    i -> i < this.fluidInventory.getMaxFluidTypes());

    public MBControllerBlockEntity(BlockPos pos, BlockState state) {
        super(CastingMBBlockEntities.MB_CONTROLLER_BLOCK_ENTITY.get(), pos, state);

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                if (index < 100) return progress[index];
                if (index < 200) return maxProgress[index - 100];
                if (index == 200) return temperature.orElse(Integer.MIN_VALUE);
                if (index == 201) return fluidInventory.getCapacityAsInt(0, FluidResource.EMPTY);
                if (index == 202) return maxItemSlots;
                return 0;
            }

            @Override
            public void set(int index, int value) {
                if (index < 100) progress[index] = value;
                else if (index < 200) maxProgress[index - 100] = value;
                else if (index == 200) {
                    temperature = (value == Integer.MIN_VALUE) ? OptionalInt.empty() : OptionalInt.of(value);
                }
                else if (index == 201) {
                    fluidInventory.setTotalCapacity(value);
                }
                else if (index == 202) {
                    maxItemSlots = value;
                }
            }

            @Override
            public int getCount() {
                return 203;
            }
        };
    }

    public int getMaxItemSlots() {
        return this.maxItemSlots;
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;

        refreshRecipeCachesIfNeeded();

        if (level.getGameTime() % 100 == 0) {
            structureDirty = true;
        }

        if (structureDirty) {
            validateMultiblock();
            if (cachedMultiblockData != null) {
                multiblockBlockPos.clear();
                multiblockBlockPos.addAll(cachedMultiblockData.allBlockPositions());
                updateMultiblockStats();
            }
            structureDirty = false;
        }

        boolean isRunning = level.getBlockState(worldPosition).getValue(MBControllerBlock.RUNNING);

        if (isRunning && cachedMultiblockData != null) {
            tickEntityDamage();
        }

        MBTankBlockEntity activeFuelTank = getActiveFuelTank();
        this.temperature = activeFuelTank != null ? activeFuelTank.getFuelTemp() : OptionalInt.empty();

        if (!isRunning || temperature.isEmpty()) {
            updateWorkingState(false);
            return;
        }

        int currentTemp = temperature.getAsInt();
        boolean progressChanged = false;
        boolean contentsChanged = false;
        boolean isWorking = false;

        List<FluidStack> existingFluids = new ArrayList<>();
        boolean hasEmptySlot = false;
        for (int i = 0; i < fluidInventory.getMaxFluidTypes(); i++) {
            FluidStack existing = FluidUtil.getStack(fluidInventory, i);
            if (existing.isEmpty()) hasEmptySlot = true;
            else existingFluids.add(existing);
        }

        int loopLimit = Math.min(maxItemSlots, 100);
        for (int i = 0; i < loopLimit; i++) {

            if (i >= inventory.size()) break;

            ItemStack stack = inventory.getResource(i).toStack();

            if (stack.isEmpty()) {
                if (progress[i] > 0) {
                    progress[i] = 0;
                    progressChanged = true;
                }
                continue;
            }

            RecipeHolder<MeltingRecipe> recipeHolder = getRecipeForSlot(i, stack);

            if (recipeHolder != null) {
                MeltingRecipe recipe = recipeHolder.value();

                if (currentTemp >= recipe.meltingTemp()) {
                    int max = 200;
                    int tempDiff = currentTemp - recipe.meltingTemp();
                    max -= (tempDiff / 100) * 10;
                    if (recipe.durationModifier().isPresent()) {
                        max = (int) (max * recipe.durationModifier().get());
                    }
                    maxProgress[i] = Math.max(max, 20);

                    if (canFitFluids(recipe.output(), existingFluids, hasEmptySlot)) {
                        isWorking = true;
                        progress[i]++;
                        progressChanged = true;

                        if (progress[i] >= maxProgress[i]) {
                            executeMelting(i, recipe, activeFuelTank);
                            progress[i] = 0;
                            contentsChanged = true;
                        }
                    }
                } else if (progress[i] > 0) {
                    progress[i] = 0;
                    progressChanged = true;
                }
            } else if (progress[i] > 0) {
                progress[i] = 0;
                progressChanged = true;
            }
        }

        updateWorkingState(isWorking);
        this.tickResourceSending(level, worldPosition);

        if (progressChanged || contentsChanged) {
            setChanged();
        }
        if (contentsChanged) {
            sync();
        }
    }

    private void refreshRecipeCachesIfNeeded() {
        if (level == null || level.getServer() == null) return;
        RecipeManager current = level.getServer().getRecipeManager();
        if (current != lastRecipeManager) {
            Arrays.fill(slotRecipeDirty, true);
            entityRecipeCache.clear();
            lastRecipeManager = current;
        }
    }

    private void updateMultiblockStats() {
        if (cachedMultiblockData == null) return;

        int oldMax = this.maxItemSlots;
        this.maxItemSlots = Math.min(cachedMultiblockData.volume(), 100);

        if (this.level != null) {
            this.level.invalidateCapabilities(this.worldPosition);
        }

        if (this.maxItemSlots < oldMax) {
            clampItemsToCapacity();
        }

        int newCapacity = cachedMultiblockData.volume() * 1000;
        fluidInventory.setTotalCapacity(newCapacity);

        this.maxItemSlots = cachedMultiblockData.volume();

        fluidInventory.clampFluidsToCapacity();

        this.regulatorCount = 0;

        for (BlockPos pos : cachedMultiblockData.extraBlocks()) {
            BlockState state = level.getBlockState(pos);

            if (state.is(CastingMBTags.Blocks.CONTROLLER_REGULATORS)) {
                regulatorCount++;
            }
        }

        if (regulatorCount > 0) {
            fluidInventory.setMaxFluidTypes(1 + regulatorCount);
        } else {
            fluidInventory.setMaxFluidTypes(100);
        }
    }

    private void clampItemsToCapacity() {
        if (level == null || level.isClientSide()) return;

        try (Transaction tx = Transaction.open(null)) {
            for (int i = maxItemSlots; i < inventory.size(); i++) {
                ItemResource resource = inventory.getResource(i);
                if (!resource.isEmpty()) {
                    int amount = inventory.getAmountAsInt(i);

                    Block.popResource(level, worldPosition, resource.toStack(amount));
                    int finalI = i;
                    inventory.runInternal(() -> {inventory.extract(finalI, resource, amount, tx);
                    });
                }
            }
            tx.commit();
        }
    }

    public @Nullable MBTankBlockEntity getActiveFuelTank() {
        if (level == null || cachedMultiblockData == null) return null;

        MBTankBlockEntity hottestTank = null;
        int highestTemp = Integer.MIN_VALUE;

        for (BlockPos pos : cachedTankPositions) {
            if (level.getBlockEntity(pos) instanceof MBTankBlockEntity tank) {
                if (!tank.getFluidHandler().getResource(0).isEmpty()) {
                    OptionalInt fuelTemp = tank.getFuelTemp();
                    if (fuelTemp.isPresent()) {
                        int currentTemp = fuelTemp.getAsInt();
                        if (currentTemp > highestTemp) {
                            highestTemp = currentTemp;
                            hottestTank = tank;
                        }
                    }
                }
            }
        }
        return hottestTank;
    }

    private void executeMelting(int slot, MeltingRecipe recipe, MBTankBlockEntity fuelTank) {
        FluidStack fuelStack = FluidUtil.getStack(fuelTank.getFluidHandler(), 0);
        if (fuelStack.isEmpty()) return;

        SyncableFluidHandler fuelHandler = (SyncableFluidHandler) fuelTank.getFluidHandler();

        var fuelRecipe = MBTankBlockEntity.getFuel(level, fuelStack);
        if (fuelRecipe == null) return;

        int amountToConsume = fuelRecipe.value().fluid().amount();

        try (Transaction tx = Transaction.open(null)) {

            inventory.runInternal(() -> {
                inventory.extract(slot, ItemResource.of(inventory.getResource(slot).toStack()), recipe.input().count(), tx);
            });

            fuelHandler.runInternal(() -> {
                fuelHandler.extract(0, FluidResource.of(fuelStack), amountToConsume, tx);
            });

            for (FluidStackTemplate fluid : recipe.output()) {
                fluidInventory.runInternal(() -> {
                    int remaining = fluid.amount();
                    for (int tank = 0; tank < fluidInventory.getMaxFluidTypes() && remaining > 0; tank++) {
                        remaining -= fluidInventory.insert(tank, FluidResource.of(fluid), remaining, tx);
                    }
                });
            }
            tx.commit();
        }

        fuelTank.setChanged();
        fuelTank.sync();
    }

    private void tickEntityDamage() {
        AABB interior = getInteriorAABB();
        if (interior == null) return;

        assert level != null;
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, interior);
        if (entities.isEmpty()) return;

        Set<UUID> currentIds = new HashSet<>();
        for (LivingEntity entity : entities) currentIds.add(entity.getUUID());
        entityDamageCooldowns.keySet().retainAll(currentIds);

        // Same precomputed snapshot idea as the item-melting loop
        List<FluidStack> existingFluids = new ArrayList<>();
        boolean hasEmptySlot = false;
        for (int i = 0; i < fluidInventory.getMaxFluidTypes(); i++) {
            FluidStack existing = FluidUtil.getStack(fluidInventory, i);
            if (existing.isEmpty()) hasEmptySlot = true;
            else existingFluids.add(existing);
        }

        for (LivingEntity entity : entities) {
            if (entity.isDeadOrDying()) continue;

            RecipeHolder<EntityMeltingRecipe> recipeHolder = getRecipeForEntity(entity.getType());
            if (recipeHolder == null) continue;

            EntityMeltingRecipe recipe = recipeHolder.value();

            if (!canFitFluids(recipe.output(), existingFluids, hasEmptySlot)) continue;

            int cooldown = entityDamageCooldowns.getOrDefault(entity.getUUID(), 0);
            if (cooldown > 0) {
                entityDamageCooldowns.put(entity.getUUID(), cooldown - 1);
                continue;
            }

            entity.hurtServer((ServerLevel) level, level.damageSources().magic(), recipe.damage());

            try (Transaction tx = Transaction.open(null)) {
                for (FluidStackTemplate fluid : recipe.output()) {
                    fluidInventory.runInternal(() -> {
                        int remaining = fluid.amount();
                        for (int tank = 0; tank < fluidInventory.getMaxFluidTypes() && remaining > 0; tank++) {
                            remaining -= fluidInventory.insert(tank, FluidResource.of(fluid), remaining, tx);
                        }
                    });
                }
                tx.commit();
            }

            if (recipe.durationModifier().isPresent()) {
                entityDamageCooldowns.put(entity.getUUID(), (int) (50 * recipe.durationModifier().get()));
            } else {
                entityDamageCooldowns.put(entity.getUUID(), 50);
            }
        }
    }

    private RecipeHolder<EntityMeltingRecipe> getRecipeForEntity(EntityType<?> entityType) {
        if (level == null || level.getServer() == null) return null;

        if (entityRecipeCache.containsKey(entityType)) {
            return entityRecipeCache.get(entityType);
        }

        RecipeHolder<EntityMeltingRecipe> found = null;
        for (var holder : level.getServer().getRecipeManager().recipeMap().byType(EntityMeltingRecipe.TYPE)) {
            if (holder.value().entity() == entityType) {
                found = holder;
                break;
            }
        }
        entityRecipeCache.put(entityType, found);
        return found;
    }

    private AABB getInteriorAABB() {
        if (cachedMultiblockData == null) return null;

        BlockPos a = cachedMultiblockData.topCorners().getFirst();
        BlockPos b = cachedMultiblockData.topCorners().getSecond();
        int height = cachedMultiblockData.height();

        int minX = Math.min(a.getX(), b.getX());
        int maxX = Math.max(a.getX(), b.getX());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxZ = Math.max(a.getZ(), b.getZ());
        int topY = Math.max(a.getY(), b.getY());
        int bottomY = topY - (height - 1);

        return new AABB(minX + 1, bottomY, minZ + 1, maxX, topY + 1, maxZ);
    }

    private void validateMultiblock() {
        assert level != null;
        MultiblockData foundData = CoreMultiblockDetector.findMultiblock(level, worldPosition, this.getBlockState().getBlock(), wallState -> wallState.is(CastingMBTags.Blocks.CONTROLLER_WALLS),
                floorState -> floorState.is(CastingMBTags.Blocks.CONTROLLER_FLOORS), extraValidBlocks -> extraValidBlocks.is(CastingMBTags.Blocks.CONTROLLER_EXTRA_BLOCKS), true, true, 1024, 128, 64);
        this.cachedMultiblockData = foundData;

        if (this.cachedMultiblockData != null) {
            List<BlockPos> tankPositions = new ArrayList<>();

            for (BlockPos pos : cachedMultiblockData.extraBlocks()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof MBSolidifierBlockEntity solidifier) {
                    solidifier.setController(this);
                } else if (be instanceof MBInterfaceBlockEntity mbInterface) {
                    mbInterface.setController(this);
                } else if (be instanceof MBMixerBlockEntity mixer) {
                    mixer.setController(this);
                } else if (be instanceof MBTankBlockEntity) {
                    tankPositions.add(pos);
                }
            }

            this.cachedTankPositions = tankPositions;

            MBTankBlockEntity tank = getActiveFuelTank();
            PacketDistributor.sendToAllPlayers(new SyncFuelTanks(this.worldPosition, tank != null ? tank.getBlockPos() : null));
        }
    }

    public List<BlockPos> getCachedTankPositions() {
        return cachedTankPositions;
    }

    private void updateWorkingState(boolean working) {
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(CastingBlock.WORKING) && state.getValue(CastingBlock.WORKING) != working) {
            level.setBlock(worldPosition, state.setValue(CastingBlock.WORKING, working), 3);
        }
    }

    private boolean canFitFluids(List<FluidStackTemplate> outputs, List<FluidStack> existingFluids, boolean hasEmptySlot) {
        int totalNeeded = outputs.stream().mapToInt(FluidStackTemplate::amount).sum();
        int currentTotal = fluidInventory.getTotalFluidAmount();
        int capacity = fluidInventory.getCapacityAsInt(0, FluidResource.EMPTY);

        if (currentTotal + totalNeeded > capacity) return false;

        for (FluidStackTemplate out : outputs) {
            boolean canPlace = hasEmptySlot;
            if (!canPlace) {
                for (FluidStack existing : existingFluids) {
                    if (FluidStack.isSameFluidSameComponents(existing, out)) {
                        canPlace = true;
                        break;
                    }
                }
            }
            if (!canPlace) return false;
        }
        return true;
    }

    private RecipeHolder<MeltingRecipe> getRecipeForSlot(int slot, ItemStack stack) {
        if (level == null || level.getServer() == null || stack.isEmpty()) return null;

        if (slotRecipeDirty[slot]) {
            RecipeHolder<MeltingRecipe> found = null;
            for (RecipeHolder<MeltingRecipe> holder : level.getServer().getRecipeManager().recipeMap().byType(MeltingRecipe.TYPE)) {
                if (holder.value().input().test(stack)) {
                    found = holder;
                    break;
                }
            }
            cachedSlotRecipes[slot] = found;
            slotRecipeDirty[slot] = false;
        }

        return cachedSlotRecipes[slot];
    }

    public void setClientSideFuelTankPos(BlockPos pos) {
        this.clientSideFuelTankPos = pos;
    }

    public BlockPos getClientSideFuelTankPos() {
        return this.clientSideFuelTankPos;
    }

    public boolean onPlayerUse(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (stack.getItem() instanceof FluidMoverItem) {
            int[] allTanks = java.util.stream.IntStream.range(0, fluidInventory.size()).toArray();
            return FluidMoverItem.onBlockInteract(stack, fluidInventory, allTanks, allTanks);
        }

        try (Transaction tx = Transaction.open(null)) {
            boolean result = FluidUtil.interactWithFluidHandler(player, hand, this.worldPosition, fluidInventory, tx);
            if (result) {
                tx.commit();
            }
            return result;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        inventory.serialize(output.child("inventory"));
        fluidInventory.serialize(output.child("fluidInventory"));
        output.putIntArray("progress", progress);
        output.putIntArray("maxProgress", maxProgress);
        output.putInt("temperature", temperature.orElse(Integer.MIN_VALUE));
        if (cachedMultiblockData != null) {
            cachedMultiblockData.serialize(output.child("cachedMultiblockData"));

        }
        output.putInt("regulatorCount", regulatorCount);
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        inventory.deserialize(input.childOrEmpty("inventory"));
        fluidInventory.deserialize(input.childOrEmpty("fluidInventory"));
        this.progress = input.getIntArray("progress").orElse(new int[100]);
        this.maxProgress = input.getIntArray("maxProgress").orElse(new int[100]);
        int tempVal = input.getIntOr("temperature", Integer.MIN_VALUE);
        this.temperature = (tempVal == Integer.MIN_VALUE) ? OptionalInt.empty() : OptionalInt.of(tempVal);
        this.cachedMultiblockData = MBData.from(input.childOrEmpty("cachedMultiblockData"));
        this.structureDirty = true;
        this.regulatorCount = input.getIntOr("regulatorCount", 0);
        super.loadAdditional(input);
    }

    public ItemStacksResourceHandler getItemHandler() {
        return inventory;
    }

    public FluidStacksResourceHandler getFluidHandler() {
        return fluidInventory;
    }

    public int getRegulatorCount() { return regulatorCount; }

    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MBControllerMenu(id, inv, this.worldPosition, data);
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.castingmb.mb_controller");
    }

    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        dropInventoryContents(inventory);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(CastingDataComponents.FLUIDS.get(), FluidListComponent.fromHandlers(fluidInventory));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        FluidListComponent component = components.get(CastingDataComponents.FLUIDS.get());
        if (component != null) {
            this.fluidInventory.setTotalCapacity(Integer.MAX_VALUE);
            this.fluidInventory.setMaxFluidTypes(0);

            component.applyToHandlers(fluidInventory);
            this.structureDirty = true;
        }
    }

    @Override
    public SyncableFluidHandler fluidHandler() {
        return fluidInventory;
    }

    @Override
    public int[] sendingTanks() {
        return new int[0];
    }

    @Override
    public SyncableFluidHandler receivingHandler() {
        return fluidInventory;
    }

    @Override
    public int[] acceptingTanks() {
        return new int[0];
    }
}