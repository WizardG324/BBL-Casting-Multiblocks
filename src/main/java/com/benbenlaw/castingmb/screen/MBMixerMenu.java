package com.benbenlaw.castingmb.screen;

import com.benbenlaw.castingmb.block.entity.MBMixerBlockEntity;
import com.benbenlaw.core.screen.SimpleAbstractContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;

public class MBMixerMenu extends SimpleAbstractContainerMenu {

    public MBMixerBlockEntity blockEntity;
    public Level level;
    public ContainerData data;
    public BlockPos blockPos;

    public MBMixerMenu(int containerID, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerID, inventory, extraData.readBlockPos(), new SimpleContainerData(4));
    }

    public MBMixerMenu(int containerID, Inventory inventory, BlockPos blockPos, ContainerData data) {
        super(CastingMBMenuTypes.MB_MIXER_MENU.get(), containerID, inventory, blockPos, 0);

        this.blockPos = blockPos;
        this.level = inventory.player.level();
        this.data = data;
        this.blockEntity = (MBMixerBlockEntity) this.level.getBlockEntity(blockPos);

        addDataSlots(data);
    }

    public int getScaledProgress() {
        int progress = data.get(0);
        int maxProgress = data.get(1);
        int scaledHeight = 16;

        return maxProgress != 0 && progress != 0 ? progress * scaledHeight / maxProgress : 0;
    }

    public void setSelectedRecipe(Identifier recipeId) {
        if (blockEntity != null) {
            blockEntity.setSelectedRecipe(recipeId);
        }
    }
}
