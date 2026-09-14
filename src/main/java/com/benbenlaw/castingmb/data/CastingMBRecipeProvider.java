package com.benbenlaw.castingmb.data;

import com.benbenlaw.casting.block.CastingBlocks;
import com.benbenlaw.casting.data.custom.FluidStackTemplateHelper;
import com.benbenlaw.casting.data.custom.SolidifierRecipeBuilder;
import com.benbenlaw.casting.fluid.FluidData;
import com.benbenlaw.casting.item.CastingItems;
import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.block.CastingMBBlocks;
import com.benbenlaw.castingmb.data.custom.EntityMeltingRecipeBuilder;
import com.benbenlaw.core.tag.ResourceType;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.benbenlaw.casting.data.custom.FluidStackTemplateHelper.getFluidIngredient;
import static com.benbenlaw.casting.data.custom.FluidStackTemplateHelper.getFluidStack;

public class CastingMBRecipeProvider extends RecipeProvider {

    public CastingMBRecipeProvider(HolderLookup.Provider provider, RecipeOutput output) {
        super(provider, output);
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> provider) {
            super(packOutput, provider);
        }

        @Override
        protected @NotNull RecipeProvider createRecipeProvider(HolderLookup.@NotNull Provider provider, @NotNull RecipeOutput recipeOutput) {
            return new CastingMBRecipeProvider(provider, recipeOutput);
        }

        @Override
        public @NotNull String getName() {
            return CastingMB.MOD_ID + " Recipes";
        }
    }


    @Override
    protected void buildRecipes() {

        //Reset
        shapeless(RecipeCategory.MISC, CastingMBBlocks.MB_CONTROLLER).requires(CastingMBBlocks.MB_CONTROLLER).unlockedBy("has_mb_controller", has(CastingMBBlocks.MB_CONTROLLER)).save(output);
        shapeless(RecipeCategory.MISC, CastingMBBlocks.MB_TANK).requires(CastingMBBlocks.MB_TANK).unlockedBy("has_mb_tank", has(CastingMBBlocks.MB_TANK)).save(output);

        simpleSolidifierRecipe(CastingMBBlocks.MB_REGULATOR, getFluidIngredient("molten_black_brick", 1000),
                CastingMBBlocks.MB_BLACK_BRICKS, "black_brick/mb_regulator", ResourceType.STORAGE_BLOCKS, getTempFromFluid("molten_black_brick"));

        simpleSolidifierRecipe(CastingMBBlocks.MB_BLACK_BRICKS, getFluidIngredient("molten_black_brick", 1000),
                CastingBlocks.BLACK_BRICKS, "black_brick/mb_black_brick", ResourceType.STORAGE_BLOCKS, getTempFromFluid("molten_black_brick"));

        simpleSolidifierRecipe(CastingMBBlocks.MB_CONTROLLER, getFluidIngredient("molten_black_brick", 4000),
                CastingBlocks.CONTROLLER, "black_brick/mb_controller", ResourceType.STORAGE_BLOCKS, getTempFromFluid("molten_black_brick"));

        simpleSolidifierRecipe(CastingMBBlocks.MB_SOLIDIFIER, getFluidIngredient("molten_black_brick", 2000),
                CastingBlocks.SOLIDIFIER, "black_brick/mb_solidifier", ResourceType.STORAGE_BLOCKS, getTempFromFluid("molten_black_brick"));

        simpleSolidifierRecipe(CastingMBBlocks.MB_TANK, getFluidIngredient("molten_black_brick", 2000),
                CastingBlocks.TANK, "black_brick/mb_tank", ResourceType.STORAGE_BLOCKS, getTempFromFluid("molten_black_brick"));

        simpleSolidifierRecipe(CastingMBBlocks.MB_BLACK_BRICK_GLASS, getFluidIngredient("molten_black_brick", 2000),
                CastingBlocks.BLACK_BRICK_GLASS, "black_brick/mb_black_brick_glass", ResourceType.STORAGE_BLOCKS, getTempFromFluid("molten_black_brick"));

        simpleSolidifierRecipe(CastingMBBlocks.MB_INTERFACE, getFluidIngredient("molten_black_brick", 2000),
                Items.HOPPER, "black_brick/mb_interface", ResourceType.STORAGE_BLOCKS, getTempFromFluid("molten_black_brick"));

        simpleSolidifierRecipe(CastingMBBlocks.MB_MIXER, getFluidIngredient("molten_black_brick", 2000),
                CastingMBBlocks.MB_TANK, "black_brick/mb_mixer", ResourceType.STORAGE_BLOCKS, getTempFromFluid("molten_black_brick"));

        //Entity Melting
        simpleEntityMeltingRecipe(EntityType.SNOW_GOLEM, "chilled_water", 6000, 20, 0.2);
        simpleEntityMeltingRecipe(EntityType.BLAZE, "molten_blaze", 45, 5, 1.2);
        simpleEntityMeltingRecipe(EntityType.ENDERMAN, "molten_ender", 250, 50, 3.0);


        /*
        EntityMeltingRecipeBuilder.meltingRecipesBuilder(EntityType.SNOW_GOLEM,
                List.of(FluidStackTemplateHelper.getFluidStack("molten_coal", 80)), 100, 20,  Optional.of(0.2));

         */


    }

    public void simpleEntityMeltingRecipe(EntityType<?> entity, String fluid, int fluidAmount, int damage, double durationModifier) {

        EntityMeltingRecipeBuilder.meltingRecipesBuilder(entity,
                List.of(FluidStackTemplateHelper.getFluidStack(fluid, fluidAmount)), getTempFromFluid(fluid), damage, Optional.of(durationModifier))
                .save(output);

    }

    public int getTempFromFluid(String fluidName) {
        return FluidData.FLUID_DEFINITIONS.stream().filter(data -> data.name().equals(fluidName)).findFirst().orElseThrow().fluidProduceType().temp();
    }


    public void simpleSolidifierRecipe(ItemLike block, SizedFluidIngredient fluidStack, ItemLike mold, String id, ResourceType resourceType, int temp) {
        SolidifierRecipeBuilder.solidifierRecipesBuilder(
                SizedIngredient.of(mold, 1),
                SizedIngredient.of(block.asItem(), 1),
                fluidStack,
                temp,
                Optional.of(getDurationModifier(resourceType))).save(output, id);
    }

    private double getDurationModifier(ResourceType type) {
        return switch (type) {
            case NUGGETS -> 0.2;
            case RODS, WIRES -> 0.4;
            case INGOTS, PLATES, DUSTS, GEMS -> 0.5;
            case GEARS -> 1.2;
            case STORAGE_BLOCKS -> 2.5;
            case ORES -> 1.5;
            case RAW_MATERIALS -> 1.25;
            case RAW_STORAGE_BLOCKS -> 3.0;
            default -> 1.0;
        };
    }
}
