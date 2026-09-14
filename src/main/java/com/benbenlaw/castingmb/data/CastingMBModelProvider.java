package com.benbenlaw.castingmb.data;

import com.benbenlaw.casting.Casting;
import com.benbenlaw.casting.block.CastingBlocks;
import com.benbenlaw.casting.block.custom.CastingBlock;
import com.benbenlaw.casting.data.CastingModelProvider;
import com.benbenlaw.casting.item.CastingItems;
import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.block.CastingMBBlocks;
import com.benbenlaw.castingmb.block.entity.renderer.MBTankSpecialRenderer;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.model.item.DynamicFluidContainerModel;
import net.neoforged.neoforge.common.NeoForgeMod;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.benbenlaw.casting.fluid.CastingFluids.FLUIDS_MAP;
import static net.minecraft.client.data.models.BlockModelGenerators.*;

public class CastingMBModelProvider extends ModelProvider {

    public CastingMBModelProvider(PackOutput output) {
        super(output, CastingMB.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {

        //Items

        //Blocks
        createMachineBlock(CastingMBBlocks.MB_CONTROLLER.get(), blockModels.blockStateOutput, blockModels.modelOutput);
        createMachineBlock(CastingMBBlocks.MB_SOLIDIFIER.get(), blockModels.blockStateOutput, blockModels.modelOutput);
        createMachineBlock(CastingMBBlocks.MB_REGULATOR.get(), blockModels.blockStateOutput, blockModels.modelOutput);
        createMachineBlock(CastingMBBlocks.MB_INTERFACE.get(), blockModels.blockStateOutput, blockModels.modelOutput);
        createMachineBlock(CastingMBBlocks.MB_MIXER.get(), blockModels.blockStateOutput, blockModels.modelOutput);
        blockModels.createTrivialCube(CastingMBBlocks.MB_BLACK_BRICKS.get());
        blockModels.createTrivialCube(CastingMBBlocks.MB_BLACK_BRICK_GLASS.get());

        blockModels.blockStateOutput.accept(
                createSimpleBlock(
                        CastingMBBlocks.MB_TANK.get(),
                        plainVariant(ModelLocationUtils.getModelLocation(CastingMBBlocks.MB_TANK.get()))
                )
        );

        createTankItemModel(itemModels, CastingMBBlocks.MB_TANK.get());




    }

    public void createTankItemModel(ItemModelGenerators itemModels, Block tankBlock) {
        Identifier blockModelId = ModelLocationUtils.getModelLocation(tankBlock);
        SpecialModelRenderer.Unbaked tankFluidSpecial = new MBTankSpecialRenderer.Unbaked();

        itemModels.itemModelOutput.accept(tankBlock.asItem(),
                ItemModelUtils.composite(ItemModelUtils.plainModel(blockModelId),
                        ItemModelUtils.specialModel(blockModelId, tankFluidSpecial))
        );
    }

    public void createMachineBlock(Block block, Consumer<BlockModelDefinitionGenerator> blockStateOutput, BiConsumer<Identifier, ModelInstance> modelOutput) {
        TextureMapping idleTextureMapping = (new TextureMapping()).put(TextureSlot.TOP, new Material(CastingMB.identifier("block/castingmb_top"))).put(TextureSlot.SIDE, new Material(CastingMB.identifier("block/castingmb_side"))).put(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front"));
        TextureMapping workingTextureMapping = (new TextureMapping()).put(TextureSlot.TOP, new Material(CastingMB.identifier("block/castingmb_top"))).put(TextureSlot.SIDE, new Material(CastingMB.identifier("block/castingmb_side"))).put(TextureSlot.FRONT, TextureMapping.getBlockTexture(block, "_front_working"));

        MultiVariant multivariant = plainVariant(ModelTemplates.CUBE_ORIENTABLE.create(block, idleTextureMapping, modelOutput));
        MultiVariant multivariant1 = plainVariant(ModelTemplates.CUBE_ORIENTABLE_VERTICAL.create(block, idleTextureMapping, modelOutput));

        MultiVariant workingVariant = plainVariant(ModelTemplates.CUBE_ORIENTABLE.createWithSuffix(block, "_working", workingTextureMapping, modelOutput));
        MultiVariant workingVariant1 = plainVariant(ModelTemplates.CUBE_ORIENTABLE_VERTICAL.createWithSuffix(block, "_working", workingTextureMapping, modelOutput));

        blockStateOutput.accept(
                MultiVariantGenerator.dispatch(block)
                        .with(PropertyDispatch.initial(BlockStateProperties.FACING, CastingBlock.WORKING)
                                .select(Direction.DOWN, false, multivariant1.with(X_ROT_180))
                                .select(Direction.UP, false, multivariant1)
                                .select(Direction.NORTH, false, multivariant)
                                .select(Direction.EAST, false, multivariant.with(Y_ROT_90))
                                .select(Direction.SOUTH,false, multivariant.with(Y_ROT_180))
                                .select(Direction.WEST,false, multivariant.with(Y_ROT_270))
                                .select(Direction.DOWN, true, workingVariant1.with(X_ROT_180))
                                .select(Direction.UP, true, workingVariant1)
                                .select(Direction.NORTH, true, workingVariant)
                                .select(Direction.EAST, true, workingVariant.with(Y_ROT_90))
                                .select(Direction.SOUTH,true, workingVariant.with(Y_ROT_180))
                                .select(Direction.WEST,true, workingVariant.with(Y_ROT_270))));

    }

    public void bucketItem(ItemModelGenerators itemModelGenerators, BucketItem item, Fluid fluid, boolean flipGas, boolean applyFluidLuminosity) {
        Material drip = new Material(Identifier.fromNamespaceAndPath(NeoForgeMod.MOD_ID, "item/mask/bucket_fluid_drip"));
        Material bucket = new Material(Identifier.withDefaultNamespace("item/bucket"));
        DynamicFluidContainerModel.Textures textures = new DynamicFluidContainerModel.Textures(Optional.empty(), Optional.of(bucket), Optional.of(drip), Optional.empty());
        itemModelGenerators.itemModelOutput.accept(item, new DynamicFluidContainerModel.Unbaked(textures, fluid, flipGas, false, applyFluidLuminosity));
    }
}
