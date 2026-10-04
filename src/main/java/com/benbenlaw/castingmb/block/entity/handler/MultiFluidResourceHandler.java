package com.benbenlaw.castingmb.block.entity.handler;

import com.benbenlaw.core.block.entity.SyncableBlockEntity;
import com.benbenlaw.core.block.entity.handler.fluid.SyncableFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class MultiFluidResourceHandler extends SyncableFluidHandler {
    private int totalCapacity;
    private int maxFluidTypes;
    private boolean partitioned = false;
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
            this.syncableBlockEntity.sync();
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

    public void setPartitioned(boolean partitioned) {
        if (this.partitioned != partitioned) {
            this.partitioned = partitioned;
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

        if (this.partitioned && this.maxFluidTypes > 0) {
            int perTypeCapacity = this.totalCapacity / this.maxFluidTypes;
            int currentInSlot = getAmountAsInt(index);
            int perTypeSpaceLeft = Math.max(0, perTypeCapacity - currentInSlot);
            spaceLeft = Math.min(spaceLeft, perTypeSpaceLeft);
        }

        int actualToInsert = Math.min(amount, spaceLeft);

        if (actualToInsert <= 0) return 0;

        FluidResource existingResource = getResource(index);

        if (!existingResource.isEmpty() && !existingResource.equals(resource)) {
            return 0;
        }

        if (existingResource.isEmpty() && hasFluidElsewhere(index, resource)) return 0;

        return super.insert(index, resource, actualToInsert, transaction);
    }

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

    private boolean hasFluidElsewhere(int index, FluidResource resource) {
        for (int i = 0; i < size(); i++) {
            if (i != index && resource.equals(getResource(i))) return true;
        }
        return false;
    }

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

    public boolean isPartitioned() {
        return partitioned;
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

    /**
     * Drains any slot back down to its current fair share (totalCapacity / maxFluidTypes).
     * A slot can end up over that share after regulators are removed/added (the old share
     * was bigger) or after mergeDuplicateTanks folds several slots together - left alone,
     * that one fluid keeps eating the shared total capacity and permanently starves any
     * other fluid type of room, even though its own per-type quota would otherwise fit.
     */
    public void clampFluidsToPartition() {
        if (!this.partitioned || this.maxFluidTypes <= 0) return;

        int perTypeCapacity = this.totalCapacity / this.maxFluidTypes;
        boolean changed = false;

        try (Transaction tx = Transaction.open(null)) {
            for (int i = 0; i < this.maxFluidTypes; i++) {
                int slotAmount = getAmountAsInt(i);
                if (slotAmount > perTypeCapacity) {
                    int extracted = extract(i, getResource(i), slotAmount - perTypeCapacity, tx);
                    if (extracted > 0) changed = true;
                }
            }
            tx.commit();
        }

        if (changed) {
            this.syncableBlockEntity.setChanged();
            this.syncableBlockEntity.sync();
        }
    }
}