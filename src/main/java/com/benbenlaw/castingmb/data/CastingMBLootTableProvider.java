package com.benbenlaw.castingmb.data;

import com.benbenlaw.casting.block.CastingBlocks;
import com.benbenlaw.casting.item.CastingDataComponents;
import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.block.CastingMBBlocks;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.packs.VanillaBlockLoot;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class CastingMBLootTableProvider extends VanillaBlockLoot {

    private final Set<Block> knownBlocks = new ReferenceOpenHashSet<>();

    public CastingMBLootTableProvider(HolderLookup.Provider provider) {
        super(provider);
    }

    @Override
    protected void generate() {

        this.dropWithFluidComponent(CastingMBBlocks.MB_CONTROLLER.get());
        this.dropWithFluidComponent(CastingMBBlocks.MB_TANK.get());

        this.dropWithMolds(CastingMBBlocks.MB_SOLIDIFIER.get());
        this.dropSelf(CastingMBBlocks.MB_BLACK_BRICKS.get());
        this.dropSelf(CastingMBBlocks.MB_REGULATOR.get());
        this.dropSelf(CastingMBBlocks.MB_BLACK_BRICK_GLASS.get());
        this.dropSelf(CastingMBBlocks.MB_INTERFACE.get());
        this.dropSelf(CastingMBBlocks.MB_MIXER.get());
    }


    private void dropWithFluidComponent(Block block) {
        this.add(block, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(block)
                                .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                                        .include(CastingDataComponents.FLUIDS.get())))));
    }

    private void dropWithMolds(Block block) {
        this.add(block, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(block)
                                .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                                        .include(CastingDataComponents.STORED_MOLDS.get())))));
    }

    @Override
    protected void add(@NotNull Block block, @NotNull LootTable.Builder table) {
        super.add(block, table);
        knownBlocks.add(block);
    }

    @NotNull
    @Override
    protected Iterable<Block> getKnownBlocks() {
        return knownBlocks;
    }
}