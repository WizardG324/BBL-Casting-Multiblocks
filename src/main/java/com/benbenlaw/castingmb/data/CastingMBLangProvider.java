package com.benbenlaw.castingmb.data;

import com.benbenlaw.casting.Casting;
import com.benbenlaw.castingmb.CastingMB;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

import static com.benbenlaw.casting.fluid.CastingFluids.FLUIDS_MAP;

public class CastingMBLangProvider extends LanguageProvider {

    public CastingMBLangProvider(PackOutput output) {
        super(output, CastingMB.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {

        //Creative Tab
        add("itemGroup.castingmb", "Casting - Multiblocks");

        //Blocks
        add("block.castingmb.mb_controller", "Casting Multiblock Controller");
        add("block.castingmb.mb_solidifier", "Casting Multiblock Solidifier");
        add("block.castingmb.mb_tank", "Casting Multiblock Tank");
        add("block.castingmb.mb_black_bricks", "Multiblock Black Bricks");
        add("block.castingmb.mb_regulator", "Multiblock Regulator");
        add("block.castingmb.mb_black_brick_glass", "Multiblock Black Brick Glass");
        add("block.castingmb.mb_interface", "Multiblock Interface (BETA)");
        add("block.castingmb.mb_mixer", "Multiblock Mixer (BETA)");

        //Tooltips
        add("tooltip.castingmb.stacked", "Stacked: %s");
        add("tooltip.castingmb.empty", "Empty");
        add("tooltip.castingmb.empty_space", "Free Space");
        add("tooltip.castingmb.no_fuel_mb", "No Fuel Tanks with hot enough fuel found in the multiblock structure!");
        add("tooltip.castingmb.no_fuel_cold_mb", "No Fuel Tanks with cold enough fuel found in the multiblock structure, add to speed up solidifiers!");
        add("tooltip.castingmb.entity_melting", "Deals %s damage per %s ticks");
        add("tooltip.castingmb.click_to_remove", "Click to remove set recipe");

        //JEI
        add("jei.castingmb.entity_melting", "Entity Melting");






    }

}
