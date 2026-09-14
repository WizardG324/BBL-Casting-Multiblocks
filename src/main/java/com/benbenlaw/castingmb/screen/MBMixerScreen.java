package com.benbenlaw.castingmb.screen;

import com.benbenlaw.casting.Casting;
import com.benbenlaw.casting.event.client.ClientRecipeCache;
import com.benbenlaw.casting.recipe.custom.MixingRecipe;
import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.network.packets.SelectMixerRecipePacket;
import com.benbenlaw.core.screen.util.FluidRenderingUtils;
import com.benbenlaw.core.util.MouseUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MBMixerScreen extends AbstractContainerScreen<MBMixerMenu> {

    private static final Identifier TEXTURE = CastingMB.identifier("textures/gui/mb_mixer_gui.png");
    private static final Identifier PROGRESS_FILL = Casting.identifier("controller_progress");
    private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/creative_inventory/scroller");

    private static final int GRID_COLUMNS = 5;
    private static final int VISIBLE_ROWS = 3;
    private static final int CELL_SIZE = 19;
    private static final int PREVIEW_X = 124;
    private static final int PREVIEW_Y = 52;

    private float scrollOffs = 0.0f;
    private boolean isScrolling = false;

    private List<Map.Entry<Identifier, MixingRecipe>> allRecipes = List.of();
    private List<Map.Entry<Identifier, MixingRecipe>> visibleRecipes = List.of();

    public MBMixerScreen(MBMixerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        refreshRecipeList();
        this.visibleRecipes = computeVisibleRecipes();
    }

    private void refreshRecipeList() {
        List<Map.Entry<Identifier, MixingRecipe>> found = new ArrayList<>(ClientRecipeCache.cachedMixingRecipes.entrySet());
        found.sort((a, b) -> a.getKey().toString().compareTo(b.getKey().toString()));
        this.allRecipes = found;
    }

    private List<Map.Entry<Identifier, MixingRecipe>> computeVisibleRecipes() {
        FluidStacksResourceHandler handler = menu.blockEntity.getFluidHandler();
        if (handler == null) return List.of();

        List<Map.Entry<Identifier, MixingRecipe>> visible = new ArrayList<>();
        for (Map.Entry<Identifier, MixingRecipe> entry : allRecipes) {
            boolean allSatisfied = true;
            for (SizedFluidIngredient required : entry.getValue().fluids()) {
                if (!hasFluidSatisfyingIngredient(handler, required)) {
                    allSatisfied = false;
                    break;
                }
            }
            if (allSatisfied) visible.add(entry);
        }
        return visible;
    }

    private boolean hasFluidSatisfyingIngredient(FluidStacksResourceHandler handler, SizedFluidIngredient required) {
        int totalFound = 0;

        for (int i = 0; i < handler.size(); i++) {
            FluidStack inTank = FluidUtil.getStack(handler, i);
            if (required.ingredient().test(inTank)) {
                totalFound += inTank.getAmount();
            }
        }

        return totalFound >= required.amount();
    }

    private int getMaxScroll() {
        int totalRows = (int) Math.ceil(visibleRecipes.size() / (double) GRID_COLUMNS);
        return Math.max(0, totalRows - VISIBLE_ROWS);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) return false;

        float step = 1.0f / (float) maxScroll;
        this.scrollOffs = Mth.clamp(this.scrollOffs - (float) scrollY * step, 0.0f, 1.0f);
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

        if (event.button() == 0 && visibleRecipes != null && !visibleRecipes.isEmpty()) {
            Map.Entry<Identifier, MixingRecipe> clicked = getRecipeAt(x, y, (int) event.x(), (int) event.y());
            if (clicked != null) {
                ClientPacketDistributor.sendToServer(new SelectMixerRecipePacket(clicked.getKey()));
                return true;
            }
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
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    private @org.jetbrains.annotations.Nullable Map.Entry<Identifier, MixingRecipe> getRecipeAt(int x, int y, int mouseX, int mouseY) {
        int rowOffset = (int) (this.scrollOffs * getMaxScroll());

        int relX = mouseX - (x + 8);
        int relY = mouseY - (y + 16);

        if (relX < 0 || relY < 0 || relX >= GRID_COLUMNS * CELL_SIZE || relY >= VISIBLE_ROWS * CELL_SIZE) {
            return null;
        }

        if (relX % CELL_SIZE >= 16 || relY % CELL_SIZE >= 16) {
            return null;
        }

        int col = relX / CELL_SIZE;
        int row = relY / CELL_SIZE;
        int recipeIndex = (rowOffset * GRID_COLUMNS) + (row * GRID_COLUMNS) + col;

        if (recipeIndex < 0 || recipeIndex >= visibleRecipes.size()) return null;
        return visibleRecipes.get(recipeIndex);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        super.extractBackground(guiGraphics, mouseX, mouseY, a);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

        this.visibleRecipes = computeVisibleRecipes();
        int maxScroll = getMaxScroll();
        if (this.scrollOffs > 0 && maxScroll == 0) this.scrollOffs = 0.0f;

        if (maxScroll > 0) {
            int scrollerX = x + 105;
            int scrollerY = y + 16 + (int) (39f * this.scrollOffs);
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, SCROLLER_SPRITE, scrollerX, scrollerY, 12, 15);
        }

        int rowOffset = (int) (this.scrollOffs * maxScroll);
        Identifier selectedId = menu.blockEntity.getSelectedRecipeId();

        for (int i = 0; i < GRID_COLUMNS * VISIBLE_ROWS; i++) {
            int recipeIndex = (rowOffset * GRID_COLUMNS) + i;
            if (recipeIndex >= visibleRecipes.size()) continue;

            int col = i % GRID_COLUMNS;
            int row = i / GRID_COLUMNS;
            int cellX = x + 8 + (col * CELL_SIZE);
            int cellY = y + 16 + (row * CELL_SIZE);

            Map.Entry<Identifier, MixingRecipe> entry = visibleRecipes.get(recipeIndex);
            boolean isSelected = selectedId != null && entry.getKey().equals(selectedId);

            if (isSelected) {
                guiGraphics.fill(cellX - 1, cellY - 1, cellX + 17, cellY + 17, new Color(255, 255, 100, 130).getRGB());
            }

            FluidStack outputStack = entry.getValue().outputFluid().create();
            FluidRenderingUtils.renderFluidStack(guiGraphics, outputStack, cellX, cellY, 16, 16, 0, 0);

            if (isSelected) {
                int scaledHeight = menu.getScaledProgress();
                if (scaledHeight > 0) {
                    guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS_FILL,
                            cellX, cellY + (16 - scaledHeight), 16, scaledHeight);
                }
            }
        }

        renderSelectedRecipePreview(guiGraphics, x, y);
    }

    private void renderSelectedRecipePreview(GuiGraphicsExtractor guiGraphics, int x, int y) {
        Identifier selectedId = menu.blockEntity.getSelectedRecipeId();
        if (selectedId == null) return;

        MixingRecipe recipe = ClientRecipeCache.cachedMixingRecipes.get(selectedId);
        if (recipe == null) return;

        FluidStack outputStack = recipe.outputFluid().create();
        FluidRenderingUtils.renderFluidStack(guiGraphics, outputStack, x + PREVIEW_X, y + PREVIEW_Y, 16, 16, 0, 0);

        int scaledHeight = menu.getScaledProgress();
        if (scaledHeight > 0) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS_FILL,
                    x + PREVIEW_X, y + PREVIEW_Y + (16 - scaledHeight), 16, scaledHeight);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        Optional<List<Component>> tooltipToDraw = renderSelectedPreviewTooltip(x, y, mouseX, mouseY);

        if (tooltipToDraw.isEmpty()) {
            tooltipToDraw = renderAlloyTooltip(x, y, mouseX, mouseY);
        }

        // Always draws the stacked tank (its return value is only a tooltip candidate)
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

    private Optional<List<Component>> renderAlloyTooltip(int x, int y, int mouseX, int mouseY) {
        Map.Entry<Identifier, MixingRecipe> entry = getRecipeAt(x, y, mouseX, mouseY);
        if (entry == null) return Optional.empty();

        return Optional.of(buildRecipeTooltip(entry.getValue()));
    }

    private Optional<List<Component>> renderSelectedPreviewTooltip(int x, int y, int mouseX, int mouseY) {
        if (!MouseUtil.isMouseOver(mouseX, mouseY, x + PREVIEW_X, y + PREVIEW_Y, 16, 16)) {
            return Optional.empty();
        }

        Identifier selectedId = menu.blockEntity.getSelectedRecipeId();
        if (selectedId == null) return Optional.empty();

        MixingRecipe recipe = ClientRecipeCache.cachedMixingRecipes.get(selectedId);
        if (recipe == null) return Optional.empty();

        return Optional.of(buildRecipeTooltip(recipe));
    }

    private List<Component> buildRecipeTooltip(MixingRecipe recipe) {
        FluidStack outputStack = recipe.outputFluid().create();

        List<Component> lines = new ArrayList<>();
        lines.add(outputStack.getHoverName());

        for (SizedFluidIngredient required : recipe.fluids()) {
            Fluid fluid = required.ingredient().fluids().getFirst().value();
            FluidStack requiredStack = new FluidStack(fluid, required.amount());
            lines.add(Component.literal(required.amount() + " mB ").append(requiredStack.getHoverName()));
        }

        return lines;
    }

    private Optional<List<Component>> renderStackedFluids(GuiGraphicsExtractor guiGraphics, int tankX, int tankY, int width, int height, int mouseX, int mouseY) {
        var handler = menu.blockEntity.getFluidHandler();
        if (handler == null) return Optional.empty();

        int totalCapacity = menu.data.get(2);
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

                    int regulatorCount = menu.data.get(3);
                    if (regulatorCount != 0) {
                        tooltip.add(Component.literal("Max " + regulatorCount + " Fluid Types"));
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
            } else {
                emptyTooltip.add(Component.translatable("tooltip.castingmb.empty_space"));
            }

            emptyTooltip.add(Component.literal(remaining + " / " + totalCapacity + " mB Free"));
            activeTooltip = Optional.of(emptyTooltip);
        }

        return activeTooltip;
    }
}
