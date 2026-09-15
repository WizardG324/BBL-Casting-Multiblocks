package com.benbenlaw.castingmb.screen;

import com.benbenlaw.casting.Casting;
import com.benbenlaw.castingmb.block.entity.MBTankBlockEntity;
import com.benbenlaw.castingmb.block.entity.handler.MultiFluidResourceHandler;
import com.benbenlaw.castingmb.network.packets.ChangeMoldPagePacket;
import com.benbenlaw.core.Core;
import com.benbenlaw.core.screen.util.DurationTooltip;
import com.benbenlaw.core.screen.util.FluidRenderingUtils;
import com.benbenlaw.core.util.MouseUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MBSolidifierScreen extends AbstractContainerScreen<MBSolidifierMenu> {

    private static final Identifier TEXTURE = Casting.identifier("textures/gui/solidifier_gui.png");
    private static final Identifier PROGRESS_ARROW = Core.identifier("progress_arrow");

    private final List<ItemStack> molds = new ArrayList<>();
    private final List<Integer> moldSlotIndices = new ArrayList<>();
    private int page = 0;

    public MBSolidifierScreen(MBSolidifierMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        populateMolds();
        ItemStack currentInput = ItemUtil.getStack(
                menu.blockEntity.getItemHandler(), 0);
        if (!currentInput.isEmpty()) {
            for (int i = 0; i < molds.size(); i++) {
                if (ItemStack.isSameItemSameComponents(molds.get(i), currentInput)) {
                    page = i;
                    break;
                }
            }
        }

        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            int max = menu.getMaxMoldPage();

            page--;
            if (page < 0) page = max;

            ClientPacketDistributor.sendToServer(
                    new ChangeMoldPagePacket(menu.blockEntity.getBlockPos(), page)
            );

            menu.setMoldPage(page);
        }).bounds(x + 32, y + 54, 10, 10).build());

        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            int max = menu.getMaxMoldPage();

            page++;
            if (page > max) page = 0;

            ClientPacketDistributor.sendToServer(
                    new ChangeMoldPagePacket(menu.blockEntity.getBlockPos(), page)
            );

            menu.setMoldPage(page);
        }).bounds(x + 134, y + 54, 10, 10).build());


    }

    public void populateMolds() {
        ResourceHandler<ItemResource> moldHandler = menu.blockEntity.getStoredMolds();

        molds.clear();
        moldSlotIndices.clear();

        for (int i = 0; i < moldHandler.size(); i++) {
            ItemStack stack = ItemUtil.getStack(moldHandler, i);
            if (!stack.isEmpty()) {
                molds.add(stack);
                moldSlotIndices.add(i);
            }
        }

        if (molds.isEmpty()) {
            page = 0;
            return;
        }

        if (page >= molds.size()) {
            page = 0;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        super.extractBackground(guiGraphics, mouseX, mouseY, a);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

        if (menu.isCrafting()) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS_ARROW, 24, 16, 0, 0, x + 76, y + 19, menu.getScaledProgress() + 1, 16);
        }

        renderControllerTank(guiGraphics, x + 8, y + 44, 16, 23, mouseX, mouseY, false);

        drawTankFluid(guiGraphics, menu.blockEntity.getFilterFluidHandler(), 0, x + 8, y + 20, 16, 16);

    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        DurationTooltip.renderDurationTooltip(guiGraphics, mouseX, mouseY, x, y, 161, 5, menu.data.get(0), menu.data.get(1));

        renderControllerTank(guiGraphics, x + 8, y + 44, 16, 23, mouseX, mouseY, true);

        drawTankTooltip(guiGraphics, menu.blockEntity.getFilterFluidHandler(), 0, x + 8, y + 20, 16, 16, mouseX, mouseY, "Empty Filter");

        BlockPos fuelTankPos = menu.blockEntity.getClientSideFuelTankPos();
        int fuelTankX = x + 152;
        int fuelTankY = y + 51;
        int fuelTankSize = 16;

        boolean fuelTankFound = false;

        if (fuelTankPos != null && menu.level.isLoaded(fuelTankPos)) {
            if (menu.level.getBlockEntity(fuelTankPos) instanceof MBTankBlockEntity tankBE) {
                FluidStack fuelStack = FluidUtil.getStack(tankBE.getFluidHandler(), 0);

                if (!fuelStack.isEmpty()) {
                    fuelTankFound = true;
                    int cap = tankBE.getFluidHandler().getCapacityAsInt(0, FluidResource.of(fuelStack));
                    int amt = fuelStack.getAmount();

                    int scaledHeight = (int) (((float) amt / cap) * fuelTankSize);
                    if (scaledHeight <= 0 && amt > 0) scaledHeight = 1;

                    int renderY = fuelTankY + (fuelTankSize - scaledHeight);

                    FluidRenderingUtils.renderFluidStack(guiGraphics, fuelStack, fuelTankX, renderY, fuelTankSize, scaledHeight, 0, 0);

                    if (MouseUtil.isMouseOver(mouseX, mouseY, fuelTankX, fuelTankY, fuelTankSize, fuelTankSize)) {
                        renderCustomTooltip(guiGraphics, List.of(
                                fuelStack.getHoverName(),
                                Component.literal(amt + " / " + cap + " mB")
                        ), mouseX, mouseY);
                    }
                }
            }
        }

        if (!fuelTankFound && MouseUtil.isMouseOver(mouseX, mouseY, fuelTankX, fuelTankY, fuelTankSize, fuelTankSize)) {
            renderCustomTooltip(guiGraphics, List.of(
                    Component.translatable("tooltip.castingmb.no_fuel_cold_mb")
            ), mouseX, mouseY);
        }
    }

    private void renderControllerTank(GuiGraphicsExtractor guiGraphics, int tankX, int tankY, int width, int height, int mouseX, int mouseY, boolean isTooltip) {
        var controller = menu.blockEntity.getController();
        if (controller == null) return;

        var handler = controller.getFluidHandler();
        int totalCapacity = menu.data.get(3) * 1000;

        if (totalCapacity <= 0) totalCapacity = 16000;

        int currentYOffset = 0;
        boolean foundFluidTooltip = false;

        for (int i = 0; i < handler.size(); i++) {
            FluidStack stack = FluidUtil.getStack(handler, i);
            if (!stack.isEmpty()) {
                int fluidHeight = (int) (((float) stack.getAmount() / totalCapacity) * height);
                if (fluidHeight <= 0 && stack.getAmount() > 0) fluidHeight = 1;

                int layerY = tankY + height - fluidHeight - currentYOffset;

                if (!isTooltip) {
                    FluidRenderingUtils.renderFluidStack(guiGraphics, stack, tankX, layerY, width, fluidHeight, 0, 0);
                } else {
                    if (MouseUtil.isMouseOver(mouseX, mouseY, tankX, layerY, width, fluidHeight)) {
                        List<Component> tooltipLines = new ArrayList<>();
                        tooltipLines.add(stack.getHoverName());
                        tooltipLines.add(Component.literal(stack.getAmount() + " / " + totalCapacity + " mB"));

                        renderCustomTooltip(guiGraphics, tooltipLines, mouseX, mouseY);
                        foundFluidTooltip = true;
                    }
                }
                currentYOffset += fluidHeight;
            }
        }

        if (isTooltip && !foundFluidTooltip && MouseUtil.isMouseOver(mouseX, mouseY, tankX, tankY, width, height)) {

            MultiFluidResourceHandler multiHandler  = (MultiFluidResourceHandler) controller.getFluidHandler();
            int currentFluidAmount = multiHandler.getTotalFluidAmount();
            int remaining = totalCapacity - currentFluidAmount;
            List<Component> emptyTooltip = new ArrayList<>();

            if (currentFluidAmount == 0) {
                emptyTooltip.add(Component.literal("Empty"));
            } else {
                emptyTooltip.add(Component.translatable("tooltip.castingmb.empty_space"));
            }

            emptyTooltip.add(Component.literal(remaining + " / " + totalCapacity + " mB Free"));
            renderCustomTooltip(guiGraphics, emptyTooltip, mouseX, mouseY);
        }
    }

    private void renderCustomTooltip(GuiGraphicsExtractor guiGraphics, List<Component> lines, int mouseX, int mouseY) {
        List<ClientTooltipComponent> components = lines.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();
        guiGraphics.tooltip(this.font, components, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
    }

    private void drawTankFluid(GuiGraphicsExtractor guiGraphics, @Nullable Object handler, int slot, int x, int y, int width, int height) {

        if (handler instanceof ResourceHandler<?> rawHandler) {

            @SuppressWarnings("unchecked")
            ResourceHandler<FluidResource> fluidHandler = (ResourceHandler<FluidResource>) rawHandler;

            FluidResource resource = fluidHandler.getResource(slot);

            if (!resource.isEmpty()) {
                int amount = fluidHandler.getAmountAsInt(slot);
                int capacity = fluidHandler.getCapacityAsInt(slot, resource);

                if (capacity > 0) {
                    FluidStack stack = resource.toStack(amount);
                    int displayLevel = (int) ((float) amount / (float) capacity * (float) height);
                    displayLevel = Math.min(height, displayLevel);

                    FluidRenderingUtils.renderFluidStack(guiGraphics, stack, x, y + height - displayLevel, width, displayLevel, 0, 0);
                }
            }
        }
    }

    private void drawTankTooltip(GuiGraphicsExtractor guiGraphics, @Nullable Object handler, int slot, int x, int y, int width, int height, int mouseX, int mouseY, String emptyName) {
        if (handler instanceof FluidStacksResourceHandler fluidHandler) {
            var stack = FluidUtil.getStack(fluidHandler, slot);

            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
                if (stack.isEmpty()) {
                    Component text = Component.literal(emptyName);
                    List<ClientTooltipComponent> components = List.of(ClientTooltipComponent.create(text.getVisualOrderText()));
                    guiGraphics.tooltip(this.font, components, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
                } else {
                    FluidRenderingUtils.renderFluidStackTooltip(guiGraphics, stack, fluidHandler, slot, x, y, width, height, mouseX, mouseY);
                }
            }
        }
    }
}