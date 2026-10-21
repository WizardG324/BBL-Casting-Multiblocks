package com.benbenlaw.castingmb.screen;

import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.network.packets.SyncInterfaceFluidWhitelist;
import com.benbenlaw.core.screen.util.FluidRenderingUtils;
import com.benbenlaw.core.screen.util.button.WhitelistButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Objects;

public class MBInterfaceScreen extends AbstractContainerScreen<MBInterfaceMenu> {

    private static final Identifier TEXTURE = CastingMB.identifier("textures/gui/mb_interface_gui.png");

    public MBInterfaceScreen(MBInterfaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Objects.requireNonNull(WhitelistButton.create(leftPos + 152, topPos + 25, 18, 18, menu.blockEntity)));

        addRenderableWidget(new WhitelistButton(leftPos + 152, topPos + 53, 18, 18, menu.blockEntity.isFluidWhitelist(), button -> {
            ((WhitelistButton) button).toggle();

            boolean newMode = !menu.blockEntity.isFluidWhitelist();
            menu.blockEntity.setFluidWhitelist(newMode);
            ClientPacketDistributor.sendToServer(new SyncInterfaceFluidWhitelist(menu.blockPos, newMode));
        }));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float a) {
        super.extractBackground(guiGraphics, mouseX, mouseY, a);

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        guiGraphics.text(font, Component.translatable("tooltip.castingmb.interface.item_filter"), leftPos + 8, topPos + 16, 0xFF404040, false);
        guiGraphics.text(font, Component.translatable("tooltip.castingmb.interface.fluid_filter"), leftPos + 8, topPos + 44, 0xFF404040, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        for (int i = 0; i < MBInterfaceMenu.FLUID_FILTER_SLOTS; i++) {
            FluidRenderingUtils.renderFluid(guiGraphics, menu.blockEntity.getFluidFilter(), i, leftPos, topPos,
                    8 + i * 18, 54, 16, 16, mouseX, mouseY, Component.translatable("tooltip.casting.empty_filter"));
        }
    }
}
