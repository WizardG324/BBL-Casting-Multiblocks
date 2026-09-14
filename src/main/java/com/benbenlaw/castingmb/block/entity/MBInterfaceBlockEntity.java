package com.benbenlaw.castingmb.block.entity;

import com.benbenlaw.casting.item.FluidMoverItem;
import com.benbenlaw.castingmb.block.CastingMBBlockEntities;
import com.benbenlaw.castingmb.block.entity.handler.InterfaceItemHandler;
import com.benbenlaw.core.block.entity.SyncableBlockEntity;
import com.benbenlaw.core.block.entity.handler.fluid.SyncableFluidHandler;
import com.benbenlaw.core.block.entity.handler.item.SyncableItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

public class MBInterfaceBlockEntity extends SyncableBlockEntity {

    private static final int BUFFER_SIZE = 27;
    private static final int SOLIDIFIER_OUTPUT_SLOT = 1;

    private MBControllerBlockEntity cachedController;
    private BlockPos controllerPos;

    private final SyncableItemHandler outputBuffer =
            new SyncableItemHandler(this, BUFFER_SIZE, (i, stack) -> false, i -> true);

    public MBInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(CastingMBBlockEntities.MB_INTERFACE_BLOCK_ENTITY.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;

        MBControllerBlockEntity controller = getController();
        if (controller == null) return;

        pullFromSolidifiers(controller);
    }

    private void pullFromSolidifiers(MBControllerBlockEntity controller) {
        if (controller.cachedMultiblockData == null) return;

        for (BlockPos pos : controller.cachedMultiblockData.extraBlocks()) {
            if (level.getBlockEntity(pos) instanceof MBSolidifierBlockEntity solidifier) {
                var solidifierInventory = solidifier.getItemHandler();
                ItemResource outputResource = solidifierInventory.getResource(SOLIDIFIER_OUTPUT_SLOT);
                if (outputResource.isEmpty()) continue;

                int available = solidifierInventory.getAmountAsInt(SOLIDIFIER_OUTPUT_SLOT);

                try (Transaction tx = Transaction.open(null)) {
                    int[] inserted = {0};
                    outputBuffer.runInternal(() -> {
                        int remaining = available;
                        for (int i = 0; i < outputBuffer.size() && remaining > 0; i++) {
                            remaining -= outputBuffer.insert(i, outputResource, remaining, tx);
                        }
                        inserted[0] = available - remaining;
                    });

                    if (inserted[0] > 0) {
                        int extracted = solidifierInventory.extract(SOLIDIFIER_OUTPUT_SLOT, outputResource, inserted[0], tx);
                        if (extracted == inserted[0]) {
                            tx.commit();
                        }
                    }
                }
            }
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

    public void setController(MBControllerBlockEntity controller) {
        this.cachedController = controller;
        this.controllerPos = controller.getBlockPos();
        this.setChanged();
        this.sync();
    }

    public @Nullable ResourceHandler<ItemResource> getItemHandler() {
        MBControllerBlockEntity controller = getController();
        if (controller == null) return null;
        return new InterfaceItemHandler(controller.getItemHandler(), outputBuffer);
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
            int[] allTanks = java.util.stream.IntStream.range(0, fluidHandler.size()).toArray();
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
        outputBuffer.serialize(output.child("outputBuffer"));
        if (controllerPos != null) {
            output.putLong("controller_pos", controllerPos.asLong());
        }
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        outputBuffer.deserialize(input.childOrEmpty("outputBuffer"));
        long posLong = input.getLongOr("controller_pos", 0);
        if (posLong != 0) {
            this.controllerPos = BlockPos.of(posLong);
        }
        super.loadAdditional(input);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        dropInventoryContents(outputBuffer);
    }
}
