package com.benbenlaw.castingmb.block.entity;

import com.benbenlaw.casting.block.custom.CastingBlock;
import com.benbenlaw.casting.config.CastingConfig;
import com.benbenlaw.casting.item.FluidMoverItem;
import com.benbenlaw.casting.recipe.custom.MixingRecipe;
import com.benbenlaw.castingmb.block.CastingMBBlockEntities;
import com.benbenlaw.castingmb.block.custom.MBMixerBlock;
import com.benbenlaw.castingmb.screen.MBMixerMenu;
import com.benbenlaw.core.block.entity.SyncableBlockEntity;
import com.benbenlaw.core.block.entity.handler.fluid.SyncableFluidHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.stream.IntStream;

public class MBMixerBlockEntity extends SyncableBlockEntity implements MenuProvider {

    private final ContainerData data;
    private int progress = 0;
    private int maxProgress = CastingConfig.defaultMixerSpeed.get() / 2;

    private MBControllerBlockEntity cachedController;
    private BlockPos controllerPos;

    private Identifier selectedRecipeId;
    private RecipeHolder<MixingRecipe> cachedRecipe;
    private RecipeManager lastRecipeManager;

    public MBMixerBlockEntity(BlockPos pos, BlockState state) {
        super(CastingMBBlockEntities.MB_MIXER_BLOCK_ENTITY.get(), pos, state);

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> progress;
                    case 1 -> maxProgress;
                    case 2 -> {
                        MBControllerBlockEntity controller = getController();
                        yield controller != null ? controller.getFluidHandler().getCapacityAsInt(0, FluidResource.EMPTY) : 0;
                    }
                    case 3 -> {
                        MBControllerBlockEntity controller = getController();
                        yield controller != null ? controller.getRegulatorCount() : 0;
                    }
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> progress = value;
                    case 1 -> maxProgress = value;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;

        MBControllerBlockEntity controller = getController();
        boolean isRunning = level.getBlockState(worldPosition).getValue(MBMixerBlock.RUNNING);

        if (controller == null || !isRunning) {
            updateWorkingState(false);
            if (progress > 0) {
                progress = 0;
                setChanged();
                sync();
            }
            return;
        }

        RecipeHolder<MixingRecipe> recipeHolder = getSelectedRecipe();
        boolean progressChanged = false;
        boolean contentsChanged = false;
        boolean isCurrentlyWorking = false;

        if (recipeHolder != null) {
            MixingRecipe recipe = recipeHolder.value();

            if (canMix(controller, recipe)) {
                isCurrentlyWorking = true;
                progress++;
                progressChanged = true;

                if (progress >= maxProgress) {
                    executeMixing(controller, recipe);
                    progress = 0;
                    contentsChanged = true;
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

        if (progressChanged || contentsChanged) {
            setChanged();
        }
        if (contentsChanged) {
            sync();
        }
    }

    private void updateWorkingState(boolean working) {
        BlockState currentState = level.getBlockState(worldPosition);
        if (currentState.hasProperty(CastingBlock.WORKING) && currentState.getValue(CastingBlock.WORKING) != working) {
            level.setBlock(worldPosition, currentState.setValue(CastingBlock.WORKING, working), 3);
        }
    }

    private boolean canMix(MBControllerBlockEntity controller, MixingRecipe recipe) {
        FluidStacksResourceHandler handler = controller.getFluidHandler();

        for (SizedFluidIngredient required : recipe.fluids()) {
            if (!hasFluidSatisfyingIngredient(handler, required)) return false;
        }

        FluidStack output = recipe.outputFluid().create();

        try (Transaction tx = Transaction.open(null)) {
            int remaining = output.getAmount();
            for (int i = 0; i < handler.size() && remaining > 0; i++) {
                remaining -= handler.insert(i, FluidResource.of(output), remaining, tx);
            }
            return remaining <= 0;
        }
    }

    private boolean hasFluidSatisfyingIngredient(FluidStacksResourceHandler handler, SizedFluidIngredient required) {
        int totalFound = 0;

        for (int i = 0; i < handler.size(); i++) {
            FluidStack inTank = FluidUtil.getStack(handler, i);
            if (required.ingredient().test(inTank)) {
                totalFound += inTank.getAmount();
            }
        }

        return totalFound >= required.amount();
    }

    private void executeMixing(MBControllerBlockEntity controller, MixingRecipe recipe) {
        FluidStacksResourceHandler handler = controller.getFluidHandler();

        try (Transaction tx = Transaction.open(null)) {
            for (SizedFluidIngredient required : recipe.fluids()) {
                int remainingToDrain = required.amount();

                for (int i = 0; i < handler.size() && remainingToDrain > 0; i++) {
                    FluidStack inTank = FluidUtil.getStack(handler, i);

                    if (required.ingredient().test(inTank)) {
                        int drained = handler.extract(i, FluidResource.of(inTank), remainingToDrain, tx);
                        remainingToDrain -= drained;
                    }
                }

                if (remainingToDrain > 0) return;
            }

            FluidStack outputStack = recipe.outputFluid().create();
            int remaining = outputStack.getAmount();
            for (int i = 0; i < handler.size() && remaining > 0; i++) {
                remaining -= handler.insert(i, FluidResource.of(outputStack), remaining, tx);
            }

            tx.commit();
        }

        controller.setChanged();
        controller.sync();
    }

    private RecipeHolder<MixingRecipe> getSelectedRecipe() {
        if (selectedRecipeId == null || level == null || level.getServer() == null) return null;

        RecipeManager recipeManager = level.getServer().getRecipeManager();
        if (cachedRecipe != null && recipeManager == lastRecipeManager
                && cachedRecipe.id().identifier().equals(selectedRecipeId)) {
            return cachedRecipe;
        }

        RecipeHolder<MixingRecipe> found = null;
        for (RecipeHolder<MixingRecipe> holder : recipeManager.recipeMap().byType(MixingRecipe.TYPE)) {
            if (holder.id().identifier().equals(selectedRecipeId)) {
                found = holder;
                break;
            }
        }

        cachedRecipe = found;
        lastRecipeManager = recipeManager;
        return found;
    }

    public void setSelectedRecipe(@Nullable Identifier recipeId) {
        this.selectedRecipeId = recipeId;
        this.cachedRecipe = null;
        this.progress = 0;
        this.setChanged();
        this.sync();
    }

    public @Nullable Identifier getSelectedRecipeId() {
        return selectedRecipeId;
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

    public void setController(MBControllerBlockEntity controller) {
        this.cachedController = controller;
        this.controllerPos = controller.getBlockPos();
        this.setChanged();
        this.sync();
    }

    public @Nullable FluidStacksResourceHandler getFluidHandler() {
        MBControllerBlockEntity controller = getController();
        return controller != null ? controller.getFluidHandler() : null;
    }

    public boolean onPlayerUse(Player player, InteractionHand hand) {
        MBControllerBlockEntity controller = getController();
        if (controller == null) return false;

        ItemStack stack = player.getItemInHand(hand);

        if (stack.getItem() instanceof FluidMoverItem) {
            SyncableFluidHandler fluidHandler = (SyncableFluidHandler) controller.getFluidHandler();
            int[] allTanks = IntStream.range(0, fluidHandler.size()).toArray();
            return FluidMoverItem.onBlockInteract(stack, fluidHandler, allTanks, allTanks);
        }

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
        output.putInt("progress", progress);
        output.putInt("maxProgress", maxProgress);
        if (controllerPos != null) {
            output.putLong("controller_pos", controllerPos.asLong());
        }
        if (selectedRecipeId != null) {
            output.putString("selected_recipe", selectedRecipeId.toString());
        }
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        progress = input.getIntOr("progress", 0);
        maxProgress = input.getIntOr("maxProgress", CastingConfig.defaultMixerSpeed.get());

        long posLong = input.getLongOr("controller_pos", 0);
        if (posLong != 0) {
            this.controllerPos = BlockPos.of(posLong);
        }

        String recipeIdString = input.getStringOr("selected_recipe", "");
        this.selectedRecipeId = recipeIdString.isEmpty() ? null : Identifier.parse(recipeIdString);
        this.cachedRecipe = null;

        super.loadAdditional(input);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int container, @NonNull Inventory inventory, @NonNull Player player) {
        return new MBMixerMenu(container, inventory, this.worldPosition, data);
    }

    @Override
    public @NonNull Component getDisplayName() {
        return Component.translatable("block.castingmb.mb_mixer");
    }
}
