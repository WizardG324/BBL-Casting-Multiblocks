package com.benbenlaw.castingmb.data;

import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.block.CastingMBBlocks;
import com.benbenlaw.castingmb.util.CastingMBTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class CastingMBItemTags extends ItemTagsProvider {

    public CastingMBItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, CastingMB.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {


        tag(CastingMBTags.Items.CONTROLLER_FLOORS)
                .add(CastingMBBlocks.MB_BLACK_BRICKS.get().asItem())
        ;

        tag(CastingMBTags.Items.CONTROLLER_REGULATORS)
                .add(CastingMBBlocks.MB_REGULATOR.get().asItem())
        ;

        tag(CastingMBTags.Items.CONTROLLER_WALLS)
                .add(CastingMBBlocks.MB_BLACK_BRICK_GLASS.get().asItem())
                .add(CastingMBBlocks.MB_BLACK_BRICKS.get().asItem())
                .add(CastingMBBlocks.MB_SOLIDIFIER.get().asItem())
                .add(CastingMBBlocks.MB_TANK.get().asItem())
                .add(CastingMBBlocks.MB_CONTROLLER.get().asItem())
                .add(CastingMBBlocks.MB_INTERFACE.get().asItem())
                .add(CastingMBBlocks.MB_MIXER.get().asItem())
        ;

        tag(CastingMBTags.Items.CONTROLLER_EXTRA_BLOCKS)
                .add(CastingMBBlocks.MB_BLACK_BRICK_GLASS.get().asItem())
                .add(CastingMBBlocks.MB_BLACK_BRICKS.get().asItem())
                .add(CastingMBBlocks.MB_SOLIDIFIER.get().asItem())
                .add(CastingMBBlocks.MB_TANK.get().asItem())
                .add(CastingMBBlocks.MB_CONTROLLER.get().asItem())
                .add(CastingMBBlocks.MB_REGULATOR.get().asItem())
                .add(CastingMBBlocks.MB_INTERFACE.get().asItem())
                .add(CastingMBBlocks.MB_MIXER.get().asItem())
        ;

    }
}
