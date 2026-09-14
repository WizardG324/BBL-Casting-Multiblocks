package com.benbenlaw.castingmb.block.entity;

import com.benbenlaw.casting.block.custom.CastingBlock;
import com.benbenlaw.casting.block.entity.FluidAccepting;
import com.benbenlaw.casting.block.entity.TankBlockEntity;
import com.benbenlaw.casting.config.CastingConfig;
import com.benbenlaw.casting.item.CastingDataComponents;
import com.benbenlaw.casting.item.util.FluidListComponent;
import com.benbenlaw.casting.recipe.custom.SolidifierRecipe;
import com.benbenlaw.casting.util.CastingTags;
import com.benbenlaw.castingmb.block.CastingMBBlockEntities;
import com.benbenlaw.castingmb.block.custom.MBSolidifierBlock;
import com.benbenlaw.castingmb.block.entity.handler.DynamicInputItemHandler;
import com.benbenlaw.castingmb.block.entity.handler.MultiFluidResourceHandler;
import com.benbenlaw.castingmb.network.packets.SyncFuelTanks;
import com.benbenlaw.castingmb.screen.MBSolidifierMenu;
import com.benbenlaw.core.block.entity.SyncableBlockEntity;
import com.benbenlaw.core.block.entity.handler.fluid.FilterFluidHandler;
import com.benbenlaw.core.block.entity.handler.fluid.SyncableFluidHandler;
import com.benbenlaw.core.block.entity.handler.item.SyncableItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public class MBSolidifierBlockEntity extends SyncableBlockEntity implements MenuProvider, FluidAccepting {

    private final ContainerData data;
    private int maxProgress = CastingConfig.defaultSolidifierSpeed.get();
    private int progress = 0;
    private OptionalInt temperature = OptionalInt.empty();
    private BlockPos lastSyncedFuelTankPos;

    private List<RecipeHolder<SolidifierRecipe>> cachedMoldCandidates = List.of();
    private boolean moldCacheDirty = true;
    private RecipeManager lastRecipeManager = null;

    private final SyncableItemHandler inventory = new SyncableItemHandler(this, 2, (i, stack) -> i == 0, i -> i == 1) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            if (index == INPUT_SLOT) {
                moldCacheDirty = true;
            }
            super.onContentsChanged(index, previousContents);
        }
    };

    private final SyncableItemHandler storedMolds = new SyncableItemHandler(this, 20, (i, stack) ->false, i -> false);
    private final FilterFluidHandler filterFluidHandler = new FilterFluidHandler(this, 1);

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    private MBControllerBlockEntity cachedController;
    private BlockPos controllerPos;
    private BlockPos clientSideFuelTankPos;

    public MBSolidifierBlockEntity(BlockPos pos, BlockState state) {
        super(CastingMBBlockEntities.MB_SOLIDIFIER_BLOCK_ENTITY.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> MBSolidifierBlockEntity.this.progress;
                    case 1 -> MBSolidifierBlockEntity.this.maxProgress;
                    case 2 -> MBSolidifierBlockEntity.this.temperature.orElse(Integer.MIN_VALUE);
                    case 3 -> {
                        var controller = getController();
                        yield (controller != null) ? controller.cachedMultiblockData.volume() : 0;
                    }
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> MBSolidifierBlockEntity.this.progress = value;
                    case 1 -> MBSolidifierBlockEntity.this.maxProgress = value;
                    case 2 -> MBSolidifierBlockEntity.this.temperature =
                            (value == Integer.MIN_VALUE) ? OptionalInt.empty() : OptionalInt.of(value);
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    public void setClientSideFuelTankPos(BlockPos pos) {
        this.clientSideFuelTankPos = pos;
    }

    public BlockPos getClientSideFuelTankPos() {
        return this.clientSideFuelTankPos;
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;

        MBControllerBlockEntity controller = getController();
        if (controller == null) return;

        MBTankBlockEntity coolantTank = getCoolestFuelTank();

        BlockPos coolantPos = coolantTank != null ? coolantTank.getBlockPos() : null;
        if (!java.util.Objects.equals(coolantPos, lastSyncedFuelTankPos)) {
            lastSyncedFuelTankPos = coolantPos;
            PacketDistributor.sendToAllPlayers(new SyncFuelTanks(this.worldPosition, coolantPos));
        }

        boolean isRunning = level.getBlockState(worldPosition).getValue(MBSolidifierBlock.RUNNING);


        int currentTemp = coolantTank != null ? coolantTank.getFuelTemp().orElse(20) : 20;
        this.temperature = OptionalInt.of(currentTemp);

        if (!isRunning) {
            updateWorkingState(false);
            if (progress > 0) {
                progress = 0;
                setChanged();
                sync();
            }
            return;
        }

        boolean progressChanged = false;
        boolean contentsChanged = false;
        boolean isCurrentlyWorking = false;

        ItemStack inputStack = ItemUtil.getStack(inventory, INPUT_SLOT);
        RecipeHolder<SolidifierRecipe> recipeHolder = getRecipe();

        if (canFillBucket(inputStack)) {
            isCurrentlyWorking = true;
            maxProgress = 20;
            progress++;
            progressChanged = true;

            if (progress >= maxProgress) {
                executeBucketFill();
                progress = 0;
                contentsChanged = true;
            }
        }

        else if (recipeHolder != null) {
            SolidifierRecipe recipe = recipeHolder.value();

            if (canFormOutput(recipe)) {
                isCurrentlyWorking = true;

                int baseMaxProgress = 200;
                double finalModifier = recipe.durationModifier().orElse(1.0);

                int recipeMeltingTemp = recipe.meltingTemp();
                boolean fuelBenefited = currentTemp < recipeMeltingTemp;

                if (fuelBenefited) {
                    int tempDifference = recipeMeltingTemp - currentTemp;
                    float tempBonus = (tempDifference / 25f) * 0.01f;
                    finalModifier -= tempBonus;
                }

                maxProgress = (int) (baseMaxProgress * finalModifier);
                if (maxProgress < 5) maxProgress = 5;

                FluidStack coolantStack = (fuelBenefited && coolantTank != null)
                        ? FluidUtil.getStack(coolantTank.getFluidHandler(), 0)
                        : FluidStack.EMPTY;

                boolean coolantEmpty = fuelBenefited && (coolantTank == null || coolantStack.isEmpty());

                if (coolantEmpty) {
                    if (progress > 0) {
                        progress = 0;
                        progressChanged = true;
                    }
                } else {
                    if (progress < maxProgress) {
                        progress++;
                        progressChanged = true;
                    }

                    if (progress >= maxProgress) {
                        if (!fuelBenefited) {
                            executeSolidifying(recipe, false);
                            progress = 0;
                            contentsChanged = true;
                        } else {
                            var fuelRecipeHolder = TankBlockEntity.getFuel(level, coolantStack);
                            int requiredAmount = fuelRecipeHolder != null
                                    ? fuelRecipeHolder.value().fluid().amount()
                                    : 100;

                            if (coolantStack.getAmount() >= requiredAmount) {
                                executeSolidifying(recipe, true);
                                progress = 0;
                                contentsChanged = true;
                            }
                        }
                    }
                }
            } else if (progress > 0) {
                progress = 0;
                progressChanged = true;
            }
        } else if (progress > 0) {
            progress = 0;
            progressChanged = true;
        }

        updateWorkingState(isCurrentlyWorking);

        updateWorkingState(isCurrentlyWorking);

        if (progressChanged || contentsChanged) {
            setChanged();
        }
        if (contentsChanged) {
            sync();
        }
    }

    public @Nullable MBControllerBlockEntity getController() {
        if (level == null) return null;

        if (cachedController != null && !cachedController.isRemoved()) {
            return cachedController;
        }

        if (controllerPos != null) {
            if (level.getBlockEntity(controllerPos) instanceof MBControllerBlockEntity controller) {
                this.cachedController = controller;
                return controller;
            }
        }
        return null;
    }

    public @Nullable MBTankBlockEntity getCoolestFuelTank() {
        MBControllerBlockEntity controller = getController();

        if (controller == null || controller.cachedMultiblockData == null) {
            return null;
        }

        MBTankBlockEntity coolestTank = null;
        int lowestTemp = Integer.MAX_VALUE;

        for (BlockPos pos : controller.getCachedTankPositions()) {
            assert level != null;
            if (level.getBlockEntity(pos) instanceof MBTankBlockEntity tank) {
                FluidStack fluid = FluidUtil.getStack(tank.getFluidHandler(), 0);
                if (fluid.isEmpty()) continue;

                int temp = tank.getFuelTemp().orElse(1000);
                if (temp < lowestTemp) {
                    lowestTemp = temp;
                    coolestTank = tank;
                }
            }
        }
        return coolestTank;
    }

    public void setController(MBControllerBlockEntity controller) {
        this.cachedController = controller;
        this.controllerPos = controller.getBlockPos();
        this.setChanged();
        this.sync();
    }

    private boolean canFillBucket(ItemStack inputStack) {
        MBControllerBlockEntity controller = getController();
        if (controller == null || !inputStack.is(Items.BUCKET)) return false;

        var handler = controller.getFluidHandler();

        for (int i = 0; i < handler.size(); i++) {
            FluidStack fluid = FluidUtil.getStack(handler, i);

            if (fluid.isEmpty()) continue;

            if (fluid.getAmount() < 1000) continue;

            ItemStack bucket = new ItemStack(fluid.getFluid().getBucket());
            if (bucket.isEmpty() || bucket.is(Items.AIR)) continue;

            return inventory.runInternal(() -> {
                try (Transaction tx = Transaction.openRoot()) {
                    long inserted = inventory.insert(OUTPUT_SLOT, ItemResource.of(bucket), 1, tx);
                    return inserted == 1;
                }
            });
        }

        return false;
    }

    private void executeBucketFill() {
        MBControllerBlockEntity controller = getController();
        if (controller == null) return;

        MultiFluidResourceHandler handler = (MultiFluidResourceHandler) controller.getFluidHandler();

        for (int i = 0; i < handler.size(); i++) {
            FluidStack fluid = FluidUtil.getStack(handler, i);
            ItemStack fullBucket = new ItemStack(fluid.getFluid().getBucket());

            if (fluid.getAmount() >= 1000 && !fullBucket.is(Items.AIR)) {

                try (Transaction tx = Transaction.openRoot()) {
                    inventory.runInternal(() -> {
                        inventory.extract(INPUT_SLOT, ItemResource.of(new ItemStack(Items.BUCKET)), 1, tx);
                        inventory.insert(OUTPUT_SLOT, ItemResource.of(fullBucket), 1, tx);
                    });

                    handler.runInternal(() -> {
                        handler.extract(0, FluidResource.of(fluid), 1000, tx);
                    });

                    tx.commit();

                }

            }
        }
    }

    private void executeSolidifying(SolidifierRecipe recipe, boolean consumeFuel) {
        MBControllerBlockEntity controller = getController();
        MBTankBlockEntity coolantTank = getCoolestFuelTank();

        if (controller == null) return;

        MultiFluidResourceHandler handler = (MultiFluidResourceHandler) controller.getFluidHandler();
        boolean coolantDrained = false;

        try (Transaction tx = Transaction.open(null)) {
            boolean metalExtracted = false;
            for (int i = 0; i < handler.size(); i++) {
                FluidStack inTank = FluidUtil.getStack(handler, i);
                if (recipe.fluid().ingredient().test(inTank)) {
                    int finalI = i;
                    handler.runInternal(() -> handler.extract(finalI, FluidResource.of(inTank), recipe.fluid().amount(), tx));
                    metalExtracted = true;
                    break;
                }
            }

            if (metalExtracted) {

                ItemStack moldStack = ItemUtil.getStack(inventory, INPUT_SLOT);

                if (!moldStack.is(CastingTags.Items.MOLDS)) {
                    inventory.runInternal(() -> {
                        inventory.extract(INPUT_SLOT, ItemResource.of(moldStack), recipe.mold().count(), tx);
                    });
                }

                if (consumeFuel && coolantTank != null) {
                    SyncableFluidHandler coolantHandler = (SyncableFluidHandler) coolantTank.getFluidHandler();
                    FluidStack fuelStack = FluidUtil.getStack(coolantHandler, 0);

                    var fuelRecipeHolder = TankBlockEntity.getFuel(level, fuelStack);
                    if (fuelRecipeHolder != null) {

                        coolantHandler.runInternal(() -> {
                            int amountToDrain = fuelRecipeHolder.value().fluid().amount();
                            coolantHandler.extract(0, FluidResource.of(fuelStack), amountToDrain, tx);
                        });
                        coolantDrained = true;
                    }
                }

                ItemStack result = getStackFromSized(recipe.output());
                if (!result.isEmpty()) {
                    inventory.runInternal(() -> {
                        inventory.insert(OUTPUT_SLOT, ItemResource.of(result), result.getCount(), tx);
                    });
                }

                tx.commit();
            }
        }

        if (coolantDrained) {
            coolantTank.setChanged();
            coolantTank.sync();
        }
    }

    private RecipeHolder<SolidifierRecipe> getRecipe() {
        MBControllerBlockEntity controller = getController();
        if (level == null || level.getServer() == null || controller == null) return null;

        ItemStack mold = ItemUtil.getStack(inventory, INPUT_SLOT);
        if (mold.isEmpty()) return null;

        RecipeManager recipeManager = level.getServer().getRecipeManager();
        if (moldCacheDirty || recipeManager != lastRecipeManager) {
            List<RecipeHolder<SolidifierRecipe>> candidates = new ArrayList<>();
            for (var holder : recipeManager.recipeMap().byType(SolidifierRecipe.TYPE)) {
                if (holder.value().mold().test(mold)) {
                    candidates.add(holder);
                }
            }
            cachedMoldCandidates = candidates;
            moldCacheDirty = false;
            lastRecipeManager = recipeManager;
        }

        if (cachedMoldCandidates.isEmpty()) return null;

        FluidResource filterResource = filterFluidHandler.getResource(0);
        var handler = controller.getFluidHandler();

        for (int i = 0; i < handler.size(); i++) {
            FluidStack fluid = FluidUtil.getStack(handler, i);
            if (fluid.isEmpty()) continue;

            if (!filterResource.isEmpty() && !FluidStack.isSameFluidSameComponents(fluid, filterResource.toStack(1))) {
                continue;
            }

            for (var holder : cachedMoldCandidates) {
                SolidifierRecipe recipe = holder.value();
                if (recipe.fluid().ingredient().test(fluid) && fluid.getAmount() >= recipe.fluid().amount()) {
                    return holder;
                }
            }
        }
        return null;
    }

    private void updateWorkingState(boolean working) {
        BlockState currentState = level.getBlockState(worldPosition);
        if (currentState.hasProperty(CastingBlock.WORKING) && currentState.getValue(CastingBlock.WORKING) != working) {
            level.setBlock(worldPosition, currentState.setValue(CastingBlock.WORKING, working), 3);
        }
    }

    private boolean canFormOutput(SolidifierRecipe recipe) {
        ItemStack recipeOutput = getStackFromSized(recipe.output());
        if (recipeOutput.isEmpty()) return false;

        return inventory.runInternal(() -> {
            try (Transaction tx = Transaction.openRoot()) {
                long inserted = inventory.insert(
                        OUTPUT_SLOT,
                        ItemResource.of(recipeOutput),
                        recipeOutput.getCount(),
                        tx
                );

                return inserted == recipeOutput.getCount();
            }
        });
    }

    private ItemStack getStackFromSized(SizedIngredient sizedIngredient) {
        return sizedIngredient.ingredient().items()
                .findFirst()
                .map(holder -> new ItemStack(holder.value(), sizedIngredient.count()))
                .orElse(ItemStack.EMPTY);
    }

    public boolean onPlayerUse(Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);

        if (stack.is(CastingTags.Items.MOLDS)) {

            for (int i = 0; i < storedMolds.size(); i++) {
                ItemStack existing = ItemUtil.getStack(storedMolds, i);

                if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                    return false;
                }
            }

            for (int i = 0; i < storedMolds.size(); i++) {
                if (storedMolds.getResource(i).isEmpty()) {

                    int slot = i;

                    storedMolds.runInternal(() -> {
                        try (Transaction tx = Transaction.open(null)) {

                            int inserted = storedMolds.insert(slot, ItemResource.of(stack), 1, tx);

                            if (inserted > 0) {
                                stack.shrink(1);
                                tx.commit();
                                return true;
                            }

                            return false;
                        }
                    });

                    return true;
                }
            }

            return true;
        }

        if (player.isCrouching() && stack.isEmpty()) {
            for (int i = 0; i < storedMolds.size(); i++) {
                if (!storedMolds.getResource(i).isEmpty()) {
                    int slot = i;
                    storedMolds.runInternal(() -> {
                        try (Transaction tx = Transaction.open(null)) {
                            ItemStack stored = ItemUtil.getStack(storedMolds, slot);
                            if (stored.isEmpty()) {
                                return false;
                            }
                            int extracted = storedMolds.extract(slot, ItemResource.of(stored), 1, tx);
                            if (extracted > 0) {
                                player.addItem(stored.copyWithCount(1));
                                tx.commit();
                                return true;
                            }

                            return false;
                        }
                    });

                    return true;
                }
            }

            return true;
        }

        MBControllerBlockEntity controller = getController();
        if (controller == null) return false;

        try (Transaction tx = Transaction.open(null)) {
            boolean result = FluidUtil.interactWithFluidHandler(player, hand, this.worldPosition, controller.getFluidHandler(), tx);
            if (result) {
                tx.commit();
            }
            return result;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        inventory.serialize(output.child("inventory"));
        filterFluidHandler.serialize(output.child("filterFluid"));
        storedMolds.serialize(output.child("storedMolds"));
        output.putInt("progress", progress);
        output.putInt("maxProgress", maxProgress);
        output.putInt("temperature", temperature.orElse(Integer.MIN_VALUE));
        if (controllerPos != null) {
            output.putLong("controller_pos", controllerPos.asLong());
        }
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        inventory.deserialize(input.childOrEmpty("inventory"));
        filterFluidHandler.deserialize(input.childOrEmpty("filterFluid"));
        storedMolds.deserialize(input.childOrEmpty("storedMolds"));
        progress = input.getIntOr("progress", 0);
        maxProgress = input.getIntOr("maxProgress", 200);
        int tempVal = input.getIntOr("temperature", Integer.MIN_VALUE);
        this.temperature = (tempVal == Integer.MIN_VALUE) ? OptionalInt.empty() : OptionalInt.of(tempVal);
        long posLong = input.getLongOr("controller_pos", 0);
        if (posLong != 0) {
            this.controllerPos = BlockPos.of(posLong);
        }
        super.loadAdditional(input);
    }

    public ItemStacksResourceHandler getItemHandler() {
        return inventory;
    }

    public FilterFluidHandler getFilterFluidHandler() { return filterFluidHandler; }
    public SyncableItemHandler getStoredMolds() { return storedMolds; }

    @Override
    public SyncableFluidHandler receivingHandler() {
        MBControllerBlockEntity controller = getController();
        if (controller == null) return null;

        var controllerHandler = controller.getFluidHandler();

        return new SyncableFluidHandler(this, 1, controllerHandler.getCapacityAsInt(0, FluidResource.EMPTY), (i, s) -> true, i -> i == 1) {
            @Override
            public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
                for (int i = 0; i < controllerHandler.size(); i++) {
                    int inserted = controllerHandler.insert(i, resource, amount, transaction);
                    if (inserted > 0) return inserted;
                }
                return 0;
            }

            @Override
            public int getCapacityAsInt(int index, FluidResource resource) {
                return controllerHandler.getCapacityAsInt(0, resource);
            }
        };
    }

    @Override
    public @Nullable FilterFluidHandler getFilter() {
        return FluidAccepting.super.getFilter();
    }

    @Override
    public int[] acceptingTanks() {
        return new int[0];
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int container, @NonNull Inventory inventory, @NonNull Player player) {
        return new MBSolidifierMenu(container, inventory, this.worldPosition, data);
    }

    @Override
    public @NonNull Component getDisplayName() { return Component.translatable("block.castingmb.mb_solidifier"); }

    @Override
    public void preRemoveSideEffects(@NonNull BlockPos pos, @NonNull BlockState state) {
        dropInventoryContents(inventory);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.@NonNull Builder builder) {
        super.collectImplicitComponents(builder);

        NonNullList<ItemStack> items = storedMolds.copyToList();
        NonNullList<ItemStack> filledItems = NonNullList.create();
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                ItemStack copy = stack.copy();
                filledItems.add(copy);
            }
        }

        builder.set(CastingDataComponents.STORED_MOLDS.get(), filledItems);
    }

    @Override
    protected void applyImplicitComponents(@NonNull DataComponentGetter components) {
        super.applyImplicitComponents(components);

        List<ItemStack> molds = components.get(CastingDataComponents.STORED_MOLDS.get());
        if (molds != null) {
            molds.forEach(stack -> {
                for (int i = 0; i < storedMolds.size(); i++) {
                    if (storedMolds.getResource(i).isEmpty()) {
                        storedMolds.set(i, ItemResource.of(stack), stack.count());
                        break;
                    }
                }
            });
        }
    }

}