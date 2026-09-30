package com.benbenlaw.castingmb.block.entity.handler;

import com.benbenlaw.castingmb.block.entity.MBInterfaceBlockEntity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;

public class InterfaceItemHandler implements ResourceHandler<ItemResource> {

    private static final int SOLIDIFIER_OUTPUT_SLOT = 1;

    private final MBInterfaceBlockEntity owner;

    public InterfaceItemHandler(MBInterfaceBlockEntity owner) {
        this.owner = owner;
    }

    private record Target(ResourceHandler<ItemResource> handler, int slot, boolean solidifier) {}

    /** The handler and slot behind an interface slot, or null for an index that no longer exists */
    private @Nullable Target resolve(int index) {
        ResourceHandler<ItemResource> controller = owner.controllerItems();
        int controllerSlots = controller == null ? 0 : controller.size();
        if (index < controllerSlots) {
            return new Target(controller, index, false);
        }
        ResourceHandler<ItemResource> solidifier = owner.solidifierOutput(index - controllerSlots);
        return solidifier == null ? null : new Target(solidifier, SOLIDIFIER_OUTPUT_SLOT, true);
    }

    @Override
    public int size() {
        ResourceHandler<ItemResource> controller = owner.controllerItems();
        return (controller == null ? 0 : controller.size()) + owner.solidifierCount();
    }

    @Override
    public ItemResource getResource(int index) {
        Target target = resolve(index);
        return target == null ? ItemResource.EMPTY : target.handler().getResource(target.slot());
    }

    @Override
    public long getAmountAsLong(int index) {
        Target target = resolve(index);
        return target == null ? 0 : target.handler().getAmountAsLong(target.slot());
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        Target target = resolve(index);
        return target == null ? 0 : target.handler().getCapacityAsLong(target.slot(), resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        Target target = resolve(index);
        return target != null && target.handler().isValid(target.slot(), resource);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Target target = resolve(index);
        return target == null ? 0 : target.handler().insert(target.slot(), resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        Target target = resolve(index);
        if (target == null) return 0;

        int extracted = target.handler().extract(target.slot(), resource, amount, transaction);
        if (extracted > 0 && target.solidifier()) {
            owner.onSolidifierExtracted(transaction);
        }
        return extracted;
    }
}
