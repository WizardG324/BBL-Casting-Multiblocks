package com.benbenlaw.castingmb.block.entity.handler;

import com.benbenlaw.core.block.entity.SyncableBlockEntity;
import com.benbenlaw.core.block.entity.handler.fluid.OutputFluidHandler;
import com.benbenlaw.core.block.entity.handler.fluid.SyncableFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class MultiFluidResourceHandler extends SyncableFluidHandler {
    private int totalCapacity;
    private int maxFluidTypes;
    // Store our own reference to bypass the private access in the parent class
    private final SyncableBlockEntity syncableBlockEntity;

    public MultiFluidResourceHandler(SyncableBlockEntity blockEntity, int maxFluidTypes, int totalCapacity, BiPredicate<Integer, FluidStack> canOutput, Predicate<Integer> canExtract) {
        super(blockEntity, 64, 1000000, canOutput, canExtract);

        this.syncableBlockEntity = blockEntity;
        this.totalCapacity = totalCapacity;
        this.maxFluidTypes = maxFluidTypes;
    }

    public void setTotalCapacity(int newCapacity) {
        if (this.totalCapacity != newCapacity) {
            this.totalCapacity = newCapacity;
            this.syncableBlockEntity.setChanged();
            this.syncableBlockEntity.sync(); // Sync to client so the GUI updates the bar height
        }
    }

    public void setMaxFluidTypes(int max) {
        int cappedMax = Math.min(max, this.size());
        if (this.maxFluidTypes != cappedMax) {
            this.maxFluidTypes = cappedMax;
            this.syncableBlockEntity.setChanged();
            this.syncableBlockEntity.sync();
        }
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) return 0;

        if (this.maxFluidTypes > 0 && index >= this.maxFluidTypes) return 0;

        int currentTotal = getTotalFluidAmount();
        int spaceLeft = Math.max(0, this.totalCapacity - currentTotal);
        int actualToInsert = Math.min(amount, spaceLeft);

        if (actualToInsert <= 0) return 0;

        FluidResource existingResource = getResource(index);

        if (!existingResource.isEmpty() && !existingResource.equals(resource)) {
            return 0;
        }

        return super.insert(index, resource, actualToInsert, transaction);
    }

    // Insert into a tank already holding this fluid before starting a new one.
    public int insertAnyTank(FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty() || amount <= 0) return 0;

        int inserted = 0;

        for (int tank = 0; tank < this.maxFluidTypes && inserted < amount; tank++) {
            if (resource.equals(getResource(tank))) {
                inserted += insert(tank, resource, amount - inserted, transaction);
            }
        }

        for (int tank = 0; tank < this.maxFluidTypes && inserted < amount; tank++) {
            if (getResource(tank).isEmpty()) {
                inserted += insert(tank, resource, amount - inserted, transaction);
            }
        }

        return inserted;
    }

    /**
     * Fold duplicate slots of the same fluid into the lowest tank holding it.
     * Moves fluids around rather than adding any, so it deliberately bypasses the total capacity check.
     */
    public void mergeDuplicateTanks() {
        if (!hasDuplicateTanks()) return;

        boolean[] changed = {false};

        runInternal(() -> {
            try (Transaction tx = Transaction.open(null)) {
                for (int i = 0; i < size(); i++) {
                    FluidResource target = getResource(i);
                    if (target.isEmpty()) continue;

                    for (int j = i + 1; j < size(); j++) {
                        if (!target.equals(getResource(j))) continue;

                        int amount = getAmountAsInt(j);
                        if (amount <= 0) continue;

                        int moved = super.insert(i, target, amount, tx);
                        if (moved > 0) {
                            super.extract(j, target, moved, tx);
                            changed[0] = true;
                        }
                    }
                }
                tx.commit();
            }
        });

        if (changed[0]) {
            this.syncableBlockEntity.setChanged();
            this.syncableBlockEntity.sync();
        }
    }

    // Cheap check, so the common case never opens a transaction
    private boolean hasDuplicateTanks() {
        Set<FluidResource> seen = new HashSet<>();

        for (int i = 0; i < size(); i++) {
            FluidResource resource = getResource(i);
            if (!resource.isEmpty() && !seen.add(resource)) return true;
        }

        return false;
    }

    @Override
    public int getCapacityAsInt(int index, FluidResource resource) {
        return this.totalCapacity;
    }

    public int getTotalFluidAmount() {
        int total = 0;
        for (int i = 0; i < this.size(); i++) {
            total += this.getAmountAsInt(i);
        }
        return total;
    }

    public int getMaxFluidTypes() {
        return maxFluidTypes;
    }

    public void clampFluidsToCapacity() {
        int currentTotal = getTotalFluidAmount();
        if (currentTotal <= this.totalCapacity) return;

        int amountToRemove = currentTotal - this.totalCapacity;

        try (Transaction tx = Transaction.open(null)) {
            for (int i = this.size() - 1; i >= 0 && amountToRemove > 0; i--) {
                int slotAmount = getAmountAsInt(i);
                if (slotAmount > 0) {
                    int toExtract = Math.min(slotAmount, amountToRemove);
                    extract(i, getResource(i), toExtract, tx);
                    amountToRemove -= toExtract;
                }
            }
            tx.commit();
        }
        this.syncableBlockEntity.setChanged();
        this.syncableBlockEntity.sync();
    }
}