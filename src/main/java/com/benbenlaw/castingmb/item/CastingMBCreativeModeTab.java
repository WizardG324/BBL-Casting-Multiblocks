package com.benbenlaw.castingmb.item;

import com.benbenlaw.casting.Casting;
import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.block.CastingMBBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class CastingMBCreativeModeTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CastingMB.MOD_ID);

    public static final Supplier<CreativeModeTab> CASTINGMB_TAB = CREATIVE_MODE_TABS.register("castingmb", () -> CreativeModeTab.builder()
            .withTabsBefore(Identifier.fromNamespaceAndPath(Casting.MOD_ID, "casting"))
            .icon(() -> CastingMBBlocks.MB_CONTROLLER.get().asItem().getDefaultInstance())
            .title(Component.translatable("itemGroup.castingmb"))
            .displayItems((featureFlagSet, output) -> {
                CastingMBItems.ITEMS.getEntries().forEach((entry) -> output.accept(entry.get()));
            }).build());
}


