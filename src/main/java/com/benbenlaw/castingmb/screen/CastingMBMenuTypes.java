package com.benbenlaw.castingmb.screen;

import com.benbenlaw.casting.Casting;
import com.benbenlaw.casting.screen.ControllerMenu;
import com.benbenlaw.casting.screen.MixerMenu;
import com.benbenlaw.casting.screen.SolidifierMenu;
import com.benbenlaw.castingmb.CastingMB;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CastingMBMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU, CastingMB.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<MBControllerMenu>> MB_CONTROLLER_MENU =
            MENUS.register("mb_controller_menu", () -> IMenuTypeExtension.create(MBControllerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MBSolidifierMenu>> MB_SOLIDIFIER_MENU =
            MENUS.register("mb_solidifier_menu", () -> IMenuTypeExtension.create(MBSolidifierMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MBMixerMenu>> MB_MIXER_MENU =
            MENUS.register("mb_mixer_menu", () -> IMenuTypeExtension.create(MBMixerMenu::new));

}