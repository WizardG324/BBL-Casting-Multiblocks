package com.benbenlaw.castingmb.block.entity.handler;

import com.benbenlaw.castingmb.block.entity.MBInterfaceBlockEntity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class InterfaceFluidHandler implements ResourceHandler<FluidResource> {

    private final MBInterfaceBlockEntity owner;

    public InterfaceFluidHandler(MBInterfaceBlockEntity owner) {
        this.owner = owner;
    }

    @Override
    public int size() {
        ResourceHandler<FluidResource> controller = owner.controllerFluids();
        return controller == null ? 0 : controller.size();
    }

    @Override
    public FluidResource getResource(int index) {
        ResourceHandler<FluidResource> controller = owner.controllerFluids();
        return controller == null ? FluidResource.EMPTY : controller.getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        ResourceHandler<FluidResource> controller = owner.controllerFluids();
        return controller == null ? 0 : controller.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        ResourceHandler<FluidResource> controller = owner.controllerFluids();
        return controller == null ? 0 : controller.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        ResourceHandler<FluidResource> controller = owner.controllerFluids();
        return controller != null && owner.allowsFluid(resource) && controller.isValid(index, resource);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        ResourceHandler<FluidResource> controller = owner.controllerFluids();
        if (controller == null || !owner.allowsFluid(resource)) return 0;
        return controller.insert(index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        ResourceHandler<FluidResource> controller = owner.controllerFluids();
        if (controller == null || !owner.allowsFluid(resource)) return 0;
        return controller.extract(index, resource, amount, transaction);
    }
}
