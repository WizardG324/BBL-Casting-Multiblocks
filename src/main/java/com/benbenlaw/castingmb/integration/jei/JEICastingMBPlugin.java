package com.benbenlaw.castingmb.integration.jei;

import com.benbenlaw.casting.integration.jei.FuelRecipeCategory;
import com.benbenlaw.casting.integration.jei.MeltingRecipeCategory;
import com.benbenlaw.casting.integration.jei.SolidifierRecipeCategory;
import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.block.CastingMBBlocks;
import com.benbenlaw.castingmb.event.client.ClientRecipeCache;
import com.benbenlaw.castingmb.screen.MBControllerScreen;
import com.benbenlaw.castingmb.screen.MBSolidifierScreen;
import com.benbenlaw.core.integration.jei.GhostFilter;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

@JeiPlugin
public class JEICastingMBPlugin implements IModPlugin {

    @Override
    public @NotNull Identifier getPluginUid() {
        return CastingMB.identifier("jei_plugin");
    }

    @Override
    public void registerIngredientAliases(IIngredientAliasRegistration registration) {
        registration.addAlias(CastingMBBlocks.MB_CONTROLLER.toStack(), "Smeltery Controller");
        registration.addAlias(CastingMBBlocks.MB_SOLIDIFIER.toStack(), "Casting Table");
        registration.addAlias(CastingMBBlocks.MB_TANK.toStack(), "Tank");
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(MeltingRecipeCategory.RECIPE_TYPE, new ItemStack(CastingMBBlocks.MB_CONTROLLER));
        registration.addCraftingStation(SolidifierRecipeCategory.RECIPE_TYPE, new ItemStack(CastingMBBlocks.MB_SOLIDIFIER));
        registration.addCraftingStation(FuelRecipeCategory.RECIPE_TYPE, new ItemStack(CastingMBBlocks.MB_TANK));
        registration.addCraftingStation(EntityMeltingRecipeCategory.RECIPE_TYPE, new ItemStack(CastingMBBlocks.MB_CONTROLLER));
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new EntityMeltingRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(EntityMeltingRecipeCategory.RECIPE_TYPE, ClientRecipeCache.getCachedEntityMeltingRecipes().stream().toList());

    }

    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(MBControllerScreen.class, 120, 30, 24, 16, MeltingRecipeCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(MBSolidifierScreen.class, 76, 19, 24, 16, SolidifierRecipeCategory.RECIPE_TYPE);

        registration.addGhostIngredientHandler(MBSolidifierScreen.class, new GhostFilter<>());
    }
}
