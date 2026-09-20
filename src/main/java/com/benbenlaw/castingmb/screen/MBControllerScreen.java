package com.benbenlaw.castingmb.screen;

import com.benbenlaw.casting.Casting;
import com.benbenlaw.castingmb.CastingMB;

import com.benbenlaw.castingmb.block.entity.MBTankBlockEntity;
import com.benbenlaw.core.screen.util.FluidRenderingUtils;
import com.benbenlaw.core.util.MouseUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MBControllerScreen extends AbstractContainerScreen<MBControllerMenu> {

    private static final Identifier TEXTURE = CastingMB.identifier("textures/gui/mb_controller_gui.png");
    private static final Identifier PROGRESS_ARROW = Casting.identifier("controller_progress");
    private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/creative_inventory/scroller");

    private float scrollOffs = 0.0f;
    private boolean isScrolling = false;
    private final int visibleRows = 3;
    private int lastSyncedSlots = -1;

    public MBControllerScreen(MBControllerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        refreshSlotPositions();
    }

    private int getMaxScroll() {
        int totalSlots = menu.data.get(202);
        int totalRows = (int) Math.ceil(totalSlots / 5.0);
        return Math.max(0, totalRows - visibleRows);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) return false;

        float step = 1.0f / (float) maxScroll;
        this.scrollOffs = Mth.clamp(this.scrollOffs - (float) scrollY * step, 0.0f, 1.0f);
        this.menu.scrollTo((int) (this.scrollOffs * maxScroll));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (event.button() == 0 && event.x() >= (x + 105) && event.x() < (x + 105 + 12) && event.y() >= (y + 16) && event.y() < (y + 16 + 52)) {
            this.isScrolling = true;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }


    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) this.isScrolling = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.isScrolling) {
            int maxScroll = getMaxScroll();
            int minY = (height - imageHeight) / 2 + 16;

            this.scrollOffs = ((float) event.y() - (float) minY - 7.5f) / 39f;
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0f, 1.0f);

            this.menu.scrollTo((int) (this.scrollOffs * maxScroll));
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        super.extractBackground(guiGraphics, mouseX, mouseY, a);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        int maxScroll = getMaxScroll();
        int intRowOffset = (int) (this.scrollOffs * maxScroll);

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

        int scrollerX = x + 105;
        int scrollerY = y + 16 + (int) (39f * this.scrollOffs);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, scrollerX, scrollerY, 12, 15);

        for (int i = 0; i < 15; i++) {
            int actualSlotIndex = (intRowOffset * 5) + i;

            if (actualSlotIndex < menu.data.get(202)) {
                int scaledHeight = menu.getScaledProgress(i, intRowOffset);

                if (scaledHeight > 0) {
                    int col = i % 5;
                    int row = i / 5;
                    int slotX = x + 8 + (col * 19);
                    int slotY = y + 16 + (row * 19);

                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS_ARROW,
                            slotX, slotY + (16 - scaledHeight), 16, scaledHeight);
                }
            } else {
                int col = i % 5;
                int row = i / 5;
                int slotX = x + 8 + (col * 19);
                int slotY = y + 16 + (row * 19);

                guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, new Color(0, 0, 0, 100).getRGB());
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        this.refreshSlotPositions();

        Optional<List<Component>> tooltipToDraw = Optional.empty();
        BlockPos fuelTankPos = menu.blockEntity.getClientSideFuelTankPos();

        if (fuelTankPos != null && menu.level.isLoaded(fuelTankPos)) {
            var be = menu.level.getBlockEntity(fuelTankPos);
            if (be instanceof MBTankBlockEntity tankBlockEntity) {
                FluidStack fuelStack = FluidUtil.getStack(tankBlockEntity.getFluidHandler(), 0);

                if (!fuelStack.isEmpty()) {
                    int capacity = tankBlockEntity.getFluidHandler().getCapacityAsInt(0, FluidResource.of(fuelStack));
                    int amount = fuelStack.getAmount();

                    int maxHeight = 16;
                    int scaledHeight = (int) (((float) amount / capacity) * maxHeight);
                    if (scaledHeight <= 0 && amount > 0) scaledHeight = 1;

                    int renderY = y + 52 + (maxHeight - scaledHeight);
                    FluidRenderingUtils.renderFluidStack(guiGraphics, fuelStack, x + 124, renderY, 16, scaledHeight, 0, 0);

                    if (MouseUtil.isMouseOver(mouseX, mouseY, x + 124, y + 52, 16, 16)) {
                        tooltipToDraw = Optional.of(List.of(
                                fuelStack.getHoverName(),
                                Component.literal(amount + " / " + capacity + " mB")
                        ));
                    }
                }
            }
        } else {
            if (MouseUtil.isMouseOver(mouseX, mouseY, x + 124, y + 52, 16, 16)) {
                tooltipToDraw = Optional.of(List.of(Component.translatable("tooltip.castingmb.no_fuel_mb")));
            }
        }

        Optional<List<Component>> stackTooltip = renderStackedFluids(guiGraphics, x + 146, y + 18, 22, 50, mouseX, mouseY);
        if (tooltipToDraw.isEmpty()) {
            tooltipToDraw = stackTooltip;
        }

        tooltipToDraw.ifPresent(lines -> {
            List<ClientTooltipComponent> tooltipComponents = lines.stream()
                    .map(Component::getVisualOrderText)
                    .map(ClientTooltipComponent::create)
                    .toList();

            guiGraphics.tooltip(this.font, tooltipComponents, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        });
    }

    private Optional<List<Component>> renderStackedFluids(GuiGraphicsExtractor guiGraphics, int tankX, int tankY, int width, int height, int mouseX, int mouseY) {
        var handler = menu.blockEntity.getFluidHandler();
        int totalCapacity = menu.data.get(201);

        if (totalCapacity <= 0) return Optional.empty();

        int currentYOffset = 0;
        int totalFluidFound = 0;
        Optional<List<Component>> activeTooltip = Optional.empty();

        int size = Math.min(handler.size(), 64);

        for (int i = 0; i < size; i++) {
            FluidStack stack = FluidUtil.getStack(handler, i);
            if (!stack.isEmpty()) {
                int amount = stack.getAmount();
                totalFluidFound += amount;

                int fluidHeight = (int) (((float) amount / totalCapacity) * height);
                if (fluidHeight <= 0 && amount > 0) fluidHeight = 1;

                int layerY = tankY + height - fluidHeight - currentYOffset;

                FluidRenderingUtils.renderFluidStack(guiGraphics, stack, tankX, layerY, width, fluidHeight, 0, 0);

                if (activeTooltip.isEmpty() &&
                        MouseUtil.isMouseOver(mouseX, mouseY, tankX, layerY, width, fluidHeight)) {

                    List<Component> tooltip = new ArrayList<>();

                    tooltip.add(stack.getHoverName());
                    tooltip.add(Component.literal(amount + " / " + totalCapacity + " mB"));

                    if (menu.blockEntity.getRegulatorCount() != 0) {
                        tooltip.add(Component.literal(
                                "Max " + menu.blockEntity.getRegulatorCount() + " Fluid Types"
                        ));
                    }

                    activeTooltip = Optional.of(tooltip);
                }

                currentYOffset += fluidHeight;
            }
        }

        if (activeTooltip.isEmpty() && MouseUtil.isMouseOver(mouseX, mouseY, tankX, tankY, width, height)) {
            int remaining = totalCapacity - totalFluidFound;
            List<Component> emptyTooltip = new ArrayList<>();

            if (totalFluidFound == 0) {
                emptyTooltip.add(Component.literal("Empty"));
                if (menu.blockEntity.getRegulatorCount() != 0) {
                    emptyTooltip.add(Component.literal(
                            "Max " + menu.blockEntity.getRegulatorCount() + " Fluid Types"
                    ));
                }
            } else {
                emptyTooltip.add(Component.translatable("tooltip.castingmb.empty_space"));
            }

            emptyTooltip.add(Component.literal(remaining + " / " + totalCapacity + " mB Free"));
            activeTooltip = Optional.of(emptyTooltip);
        }

        return activeTooltip;
    }

    private void refreshSlotPositions() {
        int currentSlots = menu.data.get(202);
        if (currentSlots != lastSyncedSlots && currentSlots > 0) {
            this.lastSyncedSlots = currentSlots;
            int maxScroll = getMaxScroll();
            this.menu.scrollTo((int) (this.scrollOffs * maxScroll));
        }
    }
}