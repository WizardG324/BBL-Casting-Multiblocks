package com.benbenlaw.castingmb.screen;

import com.benbenlaw.castingmb.block.entity.MBInterfaceBlockEntity;
import com.benbenlaw.core.screen.SimpleAbstractContainerMenu;
import com.benbenlaw.core.screen.util.slot.FilterFluidSlot;
import com.benbenlaw.core.screen.util.slot.FilterSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

public class MBInterfaceMenu extends SimpleAbstractContainerMenu {

    public static final int ITEM_FILTER_SLOTS = 8;
    public static final int FLUID_FILTER_SLOTS = 8;

    public final MBInterfaceBlockEntity blockEntity;
    public final BlockPos blockPos;

    public MBInterfaceMenu(int containerID, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerID, inventory, extraData.readBlockPos());
    }

    public MBInterfaceMenu(int containerID, Inventory inventory, BlockPos blockPos) {
        super(CastingMBMenuTypes.MB_INTERFACE_MENU.get(), containerID, inventory, blockPos, 0);

        this.blockPos = blockPos;
        this.blockEntity = (MBInterfaceBlockEntity) inventory.player.level().getBlockEntity(blockPos);
        assert blockEntity != null;

        for (int i = 0; i < ITEM_FILTER_SLOTS; i++) {
            this.addSlot(new FilterSlot(blockEntity.getItemFilter(), blockEntity.getItemFilter()::set, i, 8 + i * 18, 26));
        }

        SimpleContainer fluidFilterContainer = new SimpleContainer(FLUID_FILTER_SLOTS);
        for (int i = 0; i < FLUID_FILTER_SLOTS; i++) {
            this.addSlot(new FilterFluidSlot(fluidFilterContainer, blockEntity.getFluidFilter(), i, 8 + i * 18, 54));
        }
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size()) {
            if (this.slots.get(slotId) instanceof FilterSlot filterSlot) {
                ItemStack carried = this.getCarried();
                filterSlot.set(carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
                return;
            }

            if (this.slots.get(slotId) instanceof FilterFluidSlot filterSlot) {
                ItemStack carried = this.getCarried();
                if (carried.isEmpty()) {
                    filterSlot.setEmpty();
                } else {
                    FluidStack fluidInStack = FluidUtil.getFirstStackContained(carried);
                    if (!fluidInStack.isEmpty()) {
                        filterSlot.set(fluidInStack);
                    }
                }
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }
}
