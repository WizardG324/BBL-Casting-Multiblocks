package com.benbenlaw.castingmb.data;

import com.benbenlaw.casting.Casting;
import com.benbenlaw.casting.block.CastingBlocks;
import com.benbenlaw.casting.block.custom.CastingBlock;
import com.benbenlaw.castingmb.block.CastingMBBlocks;
import com.benbenlaw.castingmb.util.CastingMBTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class CastingMBBlockTags extends BlockTagsProvider {

    CastingMBBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, Casting.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {

        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(CastingMBBlocks.MB_BLACK_BRICK_GLASS.get())
                .add(CastingMBBlocks.MB_CONTROLLER.get())
                .add(CastingMBBlocks.MB_SOLIDIFIER.get())
                .add(CastingMBBlocks.MB_TANK.get())
                .add(CastingMBBlocks.MB_BLACK_BRICKS.get())
                .add(CastingMBBlocks.MB_REGULATOR.get())
                .add(CastingMBBlocks.MB_INTERFACE.get())
                .add(CastingMBBlocks.MB_MIXER.get())
        ;

        tag(CastingMBTags.Blocks.CONTROLLER_FLOORS)
                .add(CastingMBBlocks.MB_BLACK_BRICKS.get())
        ;

        tag(CastingMBTags.Blocks.CONTROLLER_REGULATORS)
                .add(CastingMBBlocks.MB_REGULATOR.get())
        ;

        tag(CastingMBTags.Blocks.CONTROLLER_WALLS)
                .add(CastingMBBlocks.MB_BLACK_BRICKS.get())
                .add(CastingMBBlocks.MB_SOLIDIFIER.get())
                .add(CastingMBBlocks.MB_TANK.get())
                .add(CastingMBBlocks.MB_CONTROLLER.get())
                .add(CastingMBBlocks.MB_BLACK_BRICK_GLASS.get())
                .add(CastingMBBlocks.MB_REGULATOR.get())
                .add(CastingMBBlocks.MB_INTERFACE.get())
                .add(CastingMBBlocks.MB_MIXER.get())
                ;

        tag(CastingMBTags.Blocks.CONTROLLER_EXTRA_BLOCKS)
                .add(CastingMBBlocks.MB_BLACK_BRICKS.get())
                .add(CastingMBBlocks.MB_SOLIDIFIER.get())
                .add(CastingMBBlocks.MB_TANK.get())
                .add(CastingMBBlocks.MB_CONTROLLER.get())
                .add(CastingMBBlocks.MB_REGULATOR.get())
                .add(CastingMBBlocks.MB_INTERFACE.get())
                .add(CastingMBBlocks.MB_MIXER.get())
        ;

    }

}
