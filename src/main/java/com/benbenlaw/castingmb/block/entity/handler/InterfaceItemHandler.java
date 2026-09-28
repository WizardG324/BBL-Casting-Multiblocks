package com.benbenlaw.castingmb.block.entity.handler;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.List;

public class InterfaceItemHandler implements ResourceHandler<ItemResource> {

    private static final int SOLIDIFIER_OUTPUT_SLOT = 1;

    private final ResourceHandler<ItemResource> controllerHandler;
    private final List<ResourceHandler<ItemResource>> solidifierOutputs;

    public InterfaceItemHandler(ResourceHandler<ItemResource> controllerHandler, List<ResourceHandler<ItemResource>> solidifierOutputs) {
        this.controllerHandler = controllerHandler;
        this.solidifierOutputs = solidifierOutputs;
    }

    @Override
    public int size() {
        return controllerHandler.size() + solidifierOutputs.size();
    }

    @Override
    public ItemResource getResource(int index) {
        int controllerSlots = controllerHandler.size();
        return index < controllerSlots
                ? controllerHandler.getResource(index)
                : solidifierOutputs.get(index - controllerSlots).getResource(SOLIDIFIER_OUTPUT_SLOT);
    }

    @Override
    public long getAmountAsLong(int index) {
        int controllerSlots = controllerHandler.size();
        return index < controllerSlots
                ? controllerHandler.getAmountAsLong(index)
                : solidifierOutputs.get(index - controllerSlots).getAmountAsLong(SOLIDIFIER_OUTPUT_SLOT);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        int controllerSlots = controllerHandler.size();
        return index < controllerSlots
                ? controllerHandler.getCapacityAsLong(index, resource)
                : solidifierOutputs.get(index - controllerSlots).getCapacityAsLong(SOLIDIFIER_OUTPUT_SLOT, resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        int controllerSlots = controllerHandler.size();
        return index < controllerSlots
                ? controllerHandler.isValid(index, resource)
                : solidifierOutputs.get(index - controllerSlots).isValid(SOLIDIFIER_OUTPUT_SLOT, resource);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        int controllerSlots = controllerHandler.size();
        return index < controllerSlots
                ? controllerHandler.insert(index, resource, amount, transaction)
                : solidifierOutputs.get(index - controllerSlots).insert(SOLIDIFIER_OUTPUT_SLOT, resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        int controllerSlots = controllerHandler.size();
        return index < controllerSlots
                ? controllerHandler.extract(index, resource, amount, transaction)
                : solidifierOutputs.get(index - controllerSlots).extract(SOLIDIFIER_OUTPUT_SLOT, resource, amount, transaction);
    }
}
