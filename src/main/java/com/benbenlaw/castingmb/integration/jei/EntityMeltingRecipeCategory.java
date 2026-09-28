package com.benbenlaw.castingmb.integration.jei;

import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.block.CastingMBBlocks;
import com.benbenlaw.castingmb.recipe.EntityMeltingRecipe;
import com.benbenlaw.core.util.MouseUtil;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IScrollGridWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;

import java.util.List;

public class EntityMeltingRecipeCategory implements IRecipeCategory<EntityMeltingRecipe> {

    public static final Identifier TEXTURE = CastingMB.identifier("textures/gui/entity_melting_jei.png");
    public static final IRecipeType<EntityMeltingRecipe> RECIPE_TYPE = IRecipeType.create(CastingMB.identifier("beheading"), EntityMeltingRecipe.class);

    private final int width = 86;
    private final int height = 39;
    private final IDrawable icon;

    public EntityMeltingRecipeCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(CastingMBBlocks.MB_CONTROLLER.get()));
    }

    @Override
    public @NotNull IRecipeType<EntityMeltingRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.castingmb.entity_melting");
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EntityMeltingRecipe recipe, IFocusGroup focuses) {
        int x = 68;
        int y = 2;
        int barWidth = 16;
        int barHeight = 35;

        List<FluidStackTemplate> outputs = recipe.output();
        int totalAmount = outputs.stream().mapToInt(FluidStackTemplate::amount).sum();
        if (totalAmount <= 0) return;

        int cumulativeHeight = 0;
        int remaining = outputs.size();

        for (FluidStackTemplate fluid : outputs) {
            remaining--;
            int layerHeight;
            if (remaining == 0) {
                layerHeight = barHeight - cumulativeHeight;
            } else {
                layerHeight = Math.round(barHeight * (fluid.amount() / (float) totalAmount));
            }
            if (layerHeight <= 0) continue;

            int layerY = y + (barHeight - cumulativeHeight - layerHeight);

            builder.addSlot(RecipeIngredientRole.OUTPUT, x, layerY)
                    .add(fluid.fluid().value(), fluid.amount())
                    .setFluidRenderer(fluid.amount(), false, barWidth, layerHeight);

            cumulativeHeight += layerHeight;
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, EntityMeltingRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotDrawablesView recipeSlots = builder.getRecipeSlots();
        List<IRecipeSlotDrawable> results = recipeSlots.getSlots(RecipeIngredientRole.OUTPUT);

        if (results.size() > 3) {
            IScrollGridWidget triggersGrid = builder.addScrollGridWidget(results, 2, 1);
            triggersGrid.setPosition(47, 1);
        }
    }

    public void draw(EntityMeltingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0, 0, width, height, width, height);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        LivingEntity entity = (LivingEntity) recipe.entity()
                .create(mc.level, EntitySpawnReason.MOB_SUMMONED);


        if (entity == null) return;

        Matrix3x2fStack pose = guiGraphics.pose();
        float guiLeft = pose.m20();
        float guiTop  = pose.m21();

        int x1 = (int) guiLeft + 2;
        int y1 = (int) guiTop  + 2;
        int x2 = (int) guiLeft + 35;
        int y2 = (int) guiTop  + 36;

        float areaW = x2 - x1;
        float areaH = y2 - y1;

        float entH = entity.getBbHeight();
        float entW = entity.getBbWidth();

        float entFootprint = Mth.sqrt(entW * entW + entH * entH);

        float scaleH = areaH / entH;
        float scaleW = areaW / entFootprint;

        float scale = Math.min(scaleH, scaleW);

        scale = Mth.clamp(scale, 6.0F, 18.0F);

        float yOffset = (areaH - entH * scale) / 2 / scale;

        int screenMouseX = (int) (mouseX + guiLeft);
        int screenMouseY = (int) (mouseY + guiTop);

        InventoryScreen.extractEntityInInventoryFollowsMouse(
                guiGraphics,
                x1, y1, x2, y2,
                (int) scale,
                yOffset,
                screenMouseX, screenMouseY,
                entity
        );
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, EntityMeltingRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (MouseUtil.isMouseAboveArea((int) mouseX, (int) mouseY, 38, 8, 0, 0, 28, 28)) {

            int duration;

            if (recipe.durationModifier().isPresent()) {
                duration = (int) (50 * recipe.durationModifier().get());
            } else {
                duration = 50;
            }

            tooltip.add(Component.translatable("tooltip.castingmb.entity_melting", recipe.damage(), duration));
        }
    }
}
