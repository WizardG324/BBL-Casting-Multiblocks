package com.benbenlaw.castingmb;

import com.benbenlaw.castingmb.block.CastingMBBlockEntities;
import com.benbenlaw.castingmb.block.CastingMBBlocks;
import com.benbenlaw.castingmb.block.entity.renderer.MBControllerBlockEntityRenderer;
import com.benbenlaw.castingmb.block.entity.renderer.MBTankBlockEntityRenderer;
import com.benbenlaw.castingmb.block.entity.renderer.MBTankSpecialRenderer;
import com.benbenlaw.castingmb.item.CastingMBCreativeModeTab;
import com.benbenlaw.castingmb.item.CastingMBItems;
import com.benbenlaw.castingmb.network.CastingMBNetworking;
import com.benbenlaw.castingmb.recipe.CastingMBRecipeTypes;
import com.benbenlaw.castingmb.screen.CastingMBMenuTypes;
import com.benbenlaw.castingmb.screen.MBControllerScreen;
import com.benbenlaw.castingmb.screen.MBMixerScreen;
import com.benbenlaw.castingmb.screen.MBSolidifierScreen;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(CastingMB.MOD_ID)
public class CastingMB {
    public static final String MOD_ID = "castingmb";

    public CastingMB(IEventBus modEventBus, final ModContainer modContainer) {

        CastingMBBlocks.BLOCKS.register(modEventBus);
        CastingMBBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        CastingMBItems.ITEMS.register(modEventBus);
        CastingMBCreativeModeTab.CREATIVE_MODE_TABS.register(modEventBus);
        CastingMBMenuTypes.MENUS.register(modEventBus);
        CastingMBRecipeTypes.SERIALIZER.register(modEventBus);
        CastingMBRecipeTypes.TYPES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
    }


    public void commonSetup(RegisterPayloadHandlersEvent event) {
        CastingMBNetworking.registerNetworking(event);
    }

    @EventBusSubscriber(modid = CastingMB.MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void registerSpecialModel(RegisterSpecialModelRendererEvent event) {
            event.register(CastingMB.identifier("block/mb_tank_fluid"), MBTankSpecialRenderer.Unbaked.CODEC);
        }

        @SubscribeEvent
        public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(CastingMBBlockEntities.MB_TANK_BLOCK_ENTITY.get(), MBTankBlockEntityRenderer::new);
            event.registerBlockEntityRenderer(CastingMBBlockEntities.MB_CONTROLLER_BLOCK_ENTITY.get(), MBControllerBlockEntityRenderer::new);
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(CastingMBMenuTypes.MB_CONTROLLER_MENU.get(), MBControllerScreen::new);
            event.register(CastingMBMenuTypes.MB_SOLIDIFIER_MENU.get(), MBSolidifierScreen::new);
            event.register(CastingMBMenuTypes.MB_MIXER_MENU.get(), MBMixerScreen::new);
        }

        /*
        @SubscribeEvent
        public static void onClientExtensions(RegisterClientExtensionsEvent event) {
            CastingFluids.FLUIDS_MAP.values().forEach(fluid -> {
                var fluidType = fluid.getFluidType();
                var extensions = IClientFluidTypeExtensions.of(fluidType);
                event.registerFluidType(extensions, fluidType);
            });
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

            event.enqueueWork(() -> {
                // Only Needed if i need a translucent fluid otherwise should be ok? //
                //     ItemBlockRenderTypes.setRenderLayer(ModFluids.MOLTEN_URANIUM_FLOWING.get(), RenderType.translucent());

            });
        }

        @SubscribeEvent
        public static void onKeyInput(RegisterKeyMappingsEvent event) {
            //event.register(KeyBinds.HELMET_HOTKEY);
            //event.register(KeyBinds.CHESTPLATE_HOTKEY);
            //event.register(KeyBinds.LEGGINGS_HOTKEY);
            //event.register(KeyBinds.BOOTS_HOTKEY);
        }
    }

 */

    }
    public static Identifier identifier(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
