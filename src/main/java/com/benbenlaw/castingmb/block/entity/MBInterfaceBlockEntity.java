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
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class MBInterfaceBlockEntity extends SyncableBlockEntity {

    private MBControllerBlockEntity cachedController;
    private BlockPos controllerPos;

    public MBInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(CastingMBBlockEntities.MB_INTERFACE_BLOCK_ENTITY.get(), pos, state);
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

        List<ResourceHandler<ItemResource>> solidifierOutputs = new ArrayList<>();
        if (level != null && controller.cachedMultiblockData != null) {
            for (BlockPos pos : controller.cachedMultiblockData.extraBlocks()) {
                if (level.getBlockEntity(pos) instanceof MBSolidifierBlockEntity solidifier) {
                    solidifierOutputs.add(solidifier.getItemHandler());
                }
            }
        }

        return new InterfaceItemHandler(controller.getItemHandler(), solidifierOutputs);
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
