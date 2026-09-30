package com.benbenlaw.castingmb.block.entity;

import com.benbenlaw.casting.item.FluidMoverItem;
import com.benbenlaw.castingmb.block.CastingMBBlockEntities;
import com.benbenlaw.castingmb.block.entity.handler.InterfaceItemHandler;
import com.benbenlaw.core.block.entity.SyncableBlockEntity;
import com.benbenlaw.core.block.entity.handler.fluid.SyncableFluidHandler;
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
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class MBInterfaceBlockEntity extends SyncableBlockEntity {

    private MBControllerBlockEntity cachedController;
    private BlockPos controllerPos;

    private final InterfaceItemHandler itemHandler = new InterfaceItemHandler(this);

    // Rebuilt at most once a tick
    private List<MBSolidifierBlockEntity> solidifiers = List.of();
    private long solidifiersTick = Long.MIN_VALUE;

    // Round-robin over the solidifiers, advanced once per committed transaction that took something out of one
    private int solidifierRotation;
    private boolean extractedThisTransaction;

    // Only advances when an extraction commits, so a simulated one can't skip a solidifier
    private final SnapshotJournal<Boolean> rotationJournal = new SnapshotJournal<>() {
        @Override
        protected Boolean createSnapshot() {
            return extractedThisTransaction;
        }

        @Override
        protected void revertToSnapshot(Boolean snapshot) {
            extractedThisTransaction = snapshot;
        }

        @Override
        protected void onRootCommit(Boolean originalState) {
            if (extractedThisTransaction) {
                extractedThisTransaction = false;
                solidifierRotation++;
            }
        }
    };

    public MBInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(CastingMBBlockEntities.MB_INTERFACE_BLOCK_ENTITY.get(), pos, state);
    }

    public @Nullable MBControllerBlockEntity getController() {
        if (level == null) return null;

        if (cachedController != null && !cachedController.isRemoved()) {
            return cachedController;
        }

        MBControllerBlockEntity found = null;
        if (controllerPos != null && level.getBlockEntity(controllerPos) instanceof MBControllerBlockEntity controller) {
            found = controller;
        }
        link(found);
        return found;
    }

    public void setController(MBControllerBlockEntity controller) {
        link(controller);
        this.controllerPos = controller.getBlockPos();
        this.setChanged();
        this.sync();
    }

    private void link(@Nullable MBControllerBlockEntity controller) {
        if (this.cachedController == controller) return;
        this.cachedController = controller;
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    public @Nullable ResourceHandler<ItemResource> getItemHandler() {
        return getController() != null ? itemHandler : null;
    }

    public @Nullable ResourceHandler<ItemResource> controllerItems() {
        MBControllerBlockEntity controller = getController();
        return controller != null ? controller.getItemHandler() : null;
    }

    public int solidifierCount() {
        return solidifiers().size();
    }

    // The solidifier output in round-robin order, or null if it is gone
    public @Nullable ResourceHandler<ItemResource> solidifierOutput(int i) {
        List<MBSolidifierBlockEntity> current = solidifiers();
        if (i < 0 || i >= current.size()) return null;

        MBSolidifierBlockEntity solidifier = current.get(Math.floorMod(i + solidifierRotation, current.size()));
        return solidifier.isRemoved() ? null : solidifier.getItemHandler();
    }

    public void onSolidifierExtracted(TransactionContext transaction) {
        rotationJournal.updateSnapshots(transaction);
        extractedThisTransaction = true;
    }

    private List<MBSolidifierBlockEntity> solidifiers() {
        if (level == null) return List.of();

        long now = level.getGameTime();
        if (now != solidifiersTick) {
            solidifiersTick = now;
            solidifiers = collectSolidifiers();
        }
        return solidifiers;
    }

    private List<MBSolidifierBlockEntity> collectSolidifiers() {
        MBControllerBlockEntity controller = getController();
        if (level == null || controller == null || controller.cachedMultiblockData == null) return List.of();

        List<MBSolidifierBlockEntity> found = new ArrayList<>();
        for (BlockPos pos : controller.cachedMultiblockData.extraBlocks()) {
            if (level.getBlockEntity(pos) instanceof MBSolidifierBlockEntity solidifier) {
                found.add(solidifier);
            }
        }
        return found;
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
        if (controllerPos != null) {
            output.putLong("controller_pos", controllerPos.asLong());
        }
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        long posLong = input.getLongOr("controller_pos", 0);
        if (posLong != 0) {
            this.controllerPos = BlockPos.of(posLong);
        }
        super.loadAdditional(input);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }
}
