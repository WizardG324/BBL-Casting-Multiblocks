package com.benbenlaw.castingmb.block.entity.renderer;

import com.benbenlaw.castingmb.block.entity.MBControllerBlockEntity;
import com.benbenlaw.castingmb.block.entity.handler.MultiFluidResourceHandler;
import com.benbenlaw.core.multiblock.MultiblockData;
import com.benbenlaw.core.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

public class MBControllerBlockEntityRenderer
        implements BlockEntityRenderer<MBControllerBlockEntity, MBControllerRenderState> {

    public MBControllerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public MBControllerRenderState createRenderState() {
        return new MBControllerRenderState();
    }

    @Override
    public void extractRenderState(
            MBControllerBlockEntity be,
            MBControllerRenderState state,
            float partialTick,
            net.minecraft.world.phys.Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay overlay
    ) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTick, cameraPos, overlay);

        state.fluids.clear();
        state.innerBounds = null;
        state.tankCapacity = 0;
        state.controllerPos = be.getBlockPos();

        if (be.getLevel() == null || be.cachedMultiblockData == null) {
            return;
        }
        state.lightCoords = LevelRenderer.getLightCoords(be.getLevel(), be.getBlockPos());
        MultiblockData data = be.cachedMultiblockData;

        BlockPos a = data.topCorners().getFirst();
        BlockPos b = data.topCorners().getSecond();

        int minX = Math.min(a.getX(), b.getX());
        int maxX = Math.max(a.getX(), b.getX());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxZ = Math.max(a.getZ(), b.getZ());
        int topY = Math.max(a.getY(), b.getY());
        int bottomY = topY - (data.height() - 1);

        state.innerBounds = new AABB(minX + 1, bottomY, minZ + 1, maxX, topY + 1, maxZ);
        state.controllerPos = be.getBlockPos();
        state.fluids.clear();

        MultiFluidResourceHandler handler = (MultiFluidResourceHandler) be.getFluidHandler();
        for (int i = 0; i < 20; i++) {
            FluidStack stack = FluidUtil.getStack(handler, i);
            if (!stack.isEmpty()) {
                state.fluids.add(stack.copy());
            }
        }
        state.tankCapacity = data.volume() * 1000;
    }

    @Override
    public void submit(
            MBControllerRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState
    ) {
        if (state.fluids.isEmpty() || state.tankCapacity <= 0 || state.innerBounds == null) return;

        BlockPos controllerPos = state.controllerPos;
        AABB b = state.innerBounds;
        float epsilon = 0.002f;

        int xSize = (int) (b.maxX - b.minX);
        int zSize = (int) (b.maxZ - b.minZ);
        float maxHeight = (float) (b.maxY - b.minY) - epsilon * 2;

        poseStack.pushPose();
        poseStack.translate(
                (b.minX - controllerPos.getX()) + epsilon,
                (b.minY - controllerPos.getY()) + epsilon,
                (b.minZ - controllerPos.getZ()) + epsilon
        );

        float currentY = 0f;
        for (int i = 0; i < state.fluids.size(); i++) {
            FluidStack fluid = state.fluids.get(i);
            float ratio = fluid.getAmount() / (float) state.tankCapacity;
            if (ratio <= 0.0001f) continue;

            float layerHeight = ratio * maxHeight;
            renderFluidLayerGrid(poseStack, collector, fluid, xSize, zSize, currentY, layerHeight, state.lightCoords);
            currentY += layerHeight;
        }
        poseStack.popPose();
    }

    private void renderFluidLayerGrid(
            PoseStack poseStack, SubmitNodeCollector collector, FluidStack stack,
            int xSize, int zSize, float yOffset, float layerHeight, int light
    ) {
        FluidState fluidState = stack.getFluid().defaultFluidState();
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState);
        TextureAtlasSprite texture = model.stillMaterial().sprite();
        int color = model.fluidTintSource() != null ? model.fluidTintSource().color(fluidState) : -1;

        for (int x = 0; x < xSize; x++) {
            for (int z = 0; z < zSize; z++) {

                boolean isXMin = (x == 0);
                boolean isXMax = (x == xSize - 1);
                boolean isZMin = (z == 0);
                boolean isZMax = (z == zSize - 1);

                float remainingHeight = layerHeight;
                int blockYOffset = 0;

                while (remainingHeight > 0) {
                    float currentSegmentHeight = Math.min(1.0f, remainingHeight);

                    poseStack.pushPose();
                    poseStack.translate(x, yOffset + blockYOffset, z);

                    boolean isBottom = (blockYOffset == 0);
                    boolean isTop = (remainingHeight <= 1.0f);

                    renderCube(poseStack, collector, texture, color, light,
                            isTop, isBottom, isZMin, isZMax, isXMin, isXMax, currentSegmentHeight);

                    poseStack.popPose();

                    remainingHeight -= 1.0f;
                    blockYOffset++;
                }
            }
        }
    }

    private void renderCube(
            PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite texture,
            int color, int light, boolean up, boolean down,
            boolean north, boolean south, boolean west, boolean east,
            float h
    ) {
        var sheet = Sheets.translucentBlockItemSheet();

        if (up) {
            poseStack.pushPose();
            poseStack.translate(0, h - 1.0f, 0);
            RenderUtil.submitFace(Direction.UP, poseStack, sheet, collector, texture, 0, 0, 1.0f, 1, 1, color, light);
            RenderUtil.submitFace(Direction.DOWN, poseStack, sheet, collector, texture, 0, 0, 1.0f, 1, 1, color, light);

            poseStack.popPose();
        }

        if (north) RenderUtil.submitFace(Direction.NORTH, poseStack, sheet, collector, texture, 0, 0, 0, 1, h, color, light);
        if (south) RenderUtil.submitFace(Direction.SOUTH, poseStack, sheet, collector, texture, 0, 0, 0, 1, h, color, light);
        if (west)  RenderUtil.submitFace(Direction.WEST,  poseStack, sheet, collector, texture, 0, 0, 0, 1, h, color, light);
        if (east)  RenderUtil.submitFace(Direction.EAST,  poseStack, sheet, collector, texture, 0, 0, 0, 1, h, color, light);
    }

    @Override public boolean shouldRenderOffScreen() { return true; }

    @Override
    public AABB getRenderBoundingBox(MBControllerBlockEntity be) {
        if (be.cachedMultiblockData == null) {
            return BlockEntityRenderer.super.getRenderBoundingBox(be);
        }

        var data = be.cachedMultiblockData;
        BlockPos a = data.topCorners().getFirst();
        BlockPos b = data.topCorners().getSecond();

        int minX = Math.min(a.getX(), b.getX());
        int maxX = Math.max(a.getX(), b.getX());

        int minZ = Math.min(a.getZ(), b.getZ());
        int maxZ = Math.max(a.getZ(), b.getZ());

        int topY = Math.max(a.getY(), b.getY());
        int bottomY = topY - (data.height() - 1);
        return new AABB(
                minX, bottomY, minZ,
                maxX + 1, topY + 1, maxZ + 1
        ).inflate(1.5);
    }
}