package com.benbenlaw.castingmb.block.entity.handler;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class InterfaceItemHandler implements ResourceHandler<ItemResource> {

    private final ResourceHandler<ItemResource> controllerHandler;
    private final ResourceHandler<ItemResource> outputBuffer;

    public InterfaceItemHandler(ResourceHandler<ItemResource> controllerHandler, ResourceHandler<ItemResource> outputBuffer) {
        this.controllerHandler = controllerHandler;
        this.outputBuffer = outputBuffer;
    }

    @Override
    public int size() {
        return controllerHandler.size() + outputBuffer.size();
    }

    @Override
    public ItemResource getResource(int index) {
        int controllerSlots = controllerHandler.size();
        return index < controllerSlots ? controllerHandler.getResource(index) : outputBuffer.getResource(index - controllerSlots);
    }

    @Override
    public long getAmountAsLong(int index) {
        int controllerSlots = controllerHandler.size();
        return index < controllerSlots ? controllerHandler.getAmountAsLong(index) : outputBuffer.getAmountAsLong(index - controllerSlots);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        int controllerSlots = controllerHandler.size();
        return index < controllerSlots ? controllerHandler.getCapacityAsLong(index, resource) : outputBuffer.getCapacityAsLong(index - controllerSlots, resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        int controllerSlots = controllerHandler.size();
        return index < controllerSlots ? controllerHandler.isValid(index, resource) : outputBuffer.isValid(index - controllerSlots, resource);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        int controllerSlots = controllerHandler.size();
        return index < controllerSlots ? controllerHandler.insert(index, resource, amount, transaction) : outputBuffer.insert(index - controllerSlots, resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        int controllerSlots = controllerHandler.size();
        return index < controllerSlots ? controllerHandler.extract(index, resource, amount, transaction) : outputBuffer.extract(index - controllerSlots, resource, amount, transaction);
    }
}
