package com.benbenlaw.castingmb.util;

import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.core.util.CoreTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class CastingMBTags {
    public static class Blocks extends CoreTags.Blocks {

        //Blocks
        public static final TagKey<Block> CONTROLLER_WALLS = tag(CastingMB.MOD_ID, "controller_walls");
        public static final TagKey<Block> CONTROLLER_FLOORS = tag(CastingMB.MOD_ID, "controller_floors");
        public static final TagKey<Block> CONTROLLER_EXTRA_BLOCKS = tag(CastingMB.MOD_ID, "controller_extra_blocks");
        public static final TagKey<Block> CONTROLLER_TANKS = tag(CastingMB.MOD_ID, "controller_tanks");
        public static final TagKey<Block> CONTROLLER_REGULATORS = tag(CastingMB.MOD_ID, "controller_regulators");
        public static final TagKey<Block> CONTROLLER_ALL = tag(CastingMB.MOD_ID, "controller_all");


    }

    public static class Items extends CoreTags.Items {

        public static final TagKey<Item> CONTROLLER_WALLS = tag(CastingMB.MOD_ID, "controller_walls");
        public static final TagKey<Item> CONTROLLER_FLOORS = tag(CastingMB.MOD_ID, "controller_floors");
        public static final TagKey<Item> CONTROLLER_EXTRA_BLOCKS = tag(CastingMB.MOD_ID, "controller_extra_blocks");
        public static final TagKey<Item> CONTROLLER_TANKS = tag(CastingMB.MOD_ID, "controller_tanks");
        public static final TagKey<Item> CONTROLLER_REGULATORS = tag(CastingMB.MOD_ID, "controller_regulators");
        public static final TagKey<Item> CONTROLLER_ALL = tag(CastingMB.MOD_ID, "controller_all");

    }
}
