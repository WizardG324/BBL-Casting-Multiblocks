package com.benbenlaw.castingmb.event;

import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.block.CastingMBBlockEntities;
import com.benbenlaw.castingmb.event.client.ClientRecipeCache;
import com.benbenlaw.castingmb.recipe.CastingMBRecipeTypes;
import com.benbenlaw.castingmb.recipe.EntityMeltingRecipe;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = CastingMB.MOD_ID)
public class ServerEvents {


    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {

        //MB Controller
        event.registerBlockEntity(Capabilities.Item.BLOCK, CastingMBBlockEntities.MB_CONTROLLER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getItemHandler());

        event.registerBlockEntity(Capabilities.Fluid.BLOCK, CastingMBBlockEntities.MB_CONTROLLER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getFluidHandler());

        //MB Solidifier
        event.registerBlockEntity(Capabilities.Item.BLOCK, CastingMBBlockEntities.MB_SOLIDIFIER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getItemHandler());

        //MB Tank
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, CastingMBBlockEntities.MB_TANK_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getFluidHandler());

        //MB Interface
        event.registerBlockEntity(Capabilities.Item.BLOCK, CastingMBBlockEntities.MB_INTERFACE_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getItemHandler());

        event.registerBlockEntity(Capabilities.Fluid.BLOCK, CastingMBBlockEntities.MB_INTERFACE_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getFluidHandler());

    }

    @SubscribeEvent
    public static void onDataPackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(CastingMBRecipeTypes.ENTITY_MELTING_TYPE.get());
    }

    @SubscribeEvent
    public static void onRecipeReceived(RecipesReceivedEvent event) {
        RecipeMap recipeMap = event.getRecipeMap();

        //Entity Melting
        Collection<RecipeHolder<EntityMeltingRecipe>> meltingRecipe = recipeMap.byType(CastingMBRecipeTypes.ENTITY_MELTING_TYPE.get());
        Map<Identifier, EntityMeltingRecipe> meltingRecipeMap = new HashMap<>();

        for (RecipeHolder<EntityMeltingRecipe> recipeHolder : meltingRecipe) {
            meltingRecipeMap.put(recipeHolder.id().identifier(), recipeHolder.value());
        }
        ClientRecipeCache.setCachedEntityMeltingRecipes(meltingRecipeMap);
    }


}
