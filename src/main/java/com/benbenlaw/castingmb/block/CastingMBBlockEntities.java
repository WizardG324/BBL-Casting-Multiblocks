package com.benbenlaw.castingmb.block;

import com.benbenlaw.casting.block.entity.ControllerBlockEntity;
import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.block.entity.MBControllerBlockEntity;
import com.benbenlaw.castingmb.block.entity.MBInterfaceBlockEntity;
import com.benbenlaw.castingmb.block.entity.MBMixerBlockEntity;
import com.benbenlaw.castingmb.block.entity.MBSolidifierBlockEntity;
import com.benbenlaw.castingmb.block.entity.MBTankBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class CastingMBBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CastingMB.MOD_ID);

    public static final Supplier<BlockEntityType<MBControllerBlockEntity>> MB_CONTROLLER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("mb_controller_block_entity", () ->
                    new BlockEntityType<>(MBControllerBlockEntity::new, CastingMBBlocks.MB_CONTROLLER.get()));

    public static final Supplier<BlockEntityType<MBSolidifierBlockEntity>> MB_SOLIDIFIER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("mb_solidifier_block_entity", () ->
                    new BlockEntityType<>(MBSolidifierBlockEntity::new, CastingMBBlocks.MB_SOLIDIFIER.get()));

    public static final Supplier<BlockEntityType<MBTankBlockEntity>> MB_TANK_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("mb_tank_block_entity", () ->
                    new BlockEntityType<>(MBTankBlockEntity::new, CastingMBBlocks.MB_TANK.get()));

    public static final Supplier<BlockEntityType<MBInterfaceBlockEntity>> MB_INTERFACE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("mb_interface_block_entity", () ->
                    new BlockEntityType<>(MBInterfaceBlockEntity::new, CastingMBBlocks.MB_INTERFACE.get()));

    public static final Supplier<BlockEntityType<MBMixerBlockEntity>> MB_MIXER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("mb_mixer_block_entity", () ->
                    new BlockEntityType<>(MBMixerBlockEntity::new, CastingMBBlocks.MB_MIXER.get()));

}
