package com.benbenlaw.castingmb.block;

import com.benbenlaw.casting.block.custom.CastingBlock;
import com.benbenlaw.casting.block.custom.ControllerBlock;
import com.benbenlaw.casting.item.CastingItems;
import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.block.custom.MBControllerBlock;
import com.benbenlaw.castingmb.block.custom.MBInterfaceBlock;
import com.benbenlaw.castingmb.block.custom.MBMixerBlock;
import com.benbenlaw.castingmb.block.custom.MBSolidifierBlock;
import com.benbenlaw.castingmb.block.custom.MBTankBlock;
import com.benbenlaw.castingmb.item.CastingMBItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public class CastingMBBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CastingMB.MOD_ID);

    public static final DeferredBlock<Block> MB_BLACK_BRICK_GLASS = registerBlock("mb_black_brick_glass",
            properties -> new TransparentBlock(properties
                    .strength(1.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.GLASS)));

    public static final DeferredBlock<Block> MB_BLACK_BRICKS = registerBlock("mb_black_bricks",
            properties -> new Block(properties
                    .strength(1.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final DeferredBlock<Block> MB_REGULATOR = registerBlock("mb_regulator",
            properties -> new CastingBlock(properties
                    .strength(1.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final DeferredBlock<Block> MB_CONTROLLER = registerBlock("mb_controller",
            properties -> new MBControllerBlock(properties
                    .strength(1.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final DeferredBlock<Block> MB_SOLIDIFIER = registerBlock("mb_solidifier",
        properties -> new MBSolidifierBlock(properties
                .strength(1.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion()));

    public static final DeferredBlock<Block> MB_TANK = registerBlock("mb_tank",
        properties -> new MBTankBlock(properties
                .strength(1.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion()));

    public static final DeferredBlock<Block> MB_INTERFACE = registerBlock("mb_interface",
        properties -> new MBInterfaceBlock(properties
                .strength(1.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion()));

    public static final DeferredBlock<Block> MB_MIXER = registerBlock("mb_mixer",
        properties -> new MBMixerBlock(properties
                .strength(1.0F)
                .requiresCorrectToolForDrops()
                .noOcclusion()));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        DeferredBlock<T> toReturn = BLOCKS.registerBlock(name, function);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        CastingMBItems.ITEMS.registerItem(name, properties -> new BlockItem(block.get(), properties.useBlockDescriptionPrefix()));
    }
}
