package io.wispforest.worldmesher.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.wispforest.worldmesher.mixin.ModelBlockRendererInvoker;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

public class WorldMesherBlockModelRenderer extends ModelBlockRenderer {

    private byte cullingOverrides = 0;

    public WorldMesherBlockModelRenderer() {
        super(Minecraft.getInstance().getBlockColors());
    }

    public void setCullDirection(Direction direction, boolean alwaysDraw) {
        if (!alwaysDraw) return;
        cullingOverrides |= (1 << List.of(Direction.values()).indexOf(direction));
    }

    public void clearCullingOverrides() {
        cullingOverrides = 0;
    }

    @Override
    public boolean tesselateBlock(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos, PoseStack poseStack, VertexConsumer vertexConsumer, boolean cull, Random random, long seed, int overlay) {
        boolean useAO = Minecraft.useAmbientOcclusion() && state.getLightEmission() == 0 && model.useAmbientOcclusion();
        Vec3 vec3d = state.getOffset(level, pos);
        poseStack.translate(vec3d.x, vec3d.y, vec3d.z);

        try {
            return useAO ? this.tesselateWithAO(level, model, state, pos, poseStack, vertexConsumer, cull, random, seed, overlay) : this.tesselateWithoutAO(level, model, state, pos, poseStack, vertexConsumer, cull, random, seed, overlay);
        } catch (Throwable var17) {
            CrashReport crashReport = CrashReport.forThrowable(var17, "Tesselating block model");
            CrashReportCategory crashReportSection = crashReport.addCategory("Block model being tesselated");
            CrashReportCategory.populateBlockDetails(crashReportSection, level, pos, state);
            crashReportSection.setDetail("Using AO", useAO);
            try {
                throw crashReport.getException();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean tesselateWithAO(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos, PoseStack poseStack, VertexConsumer vertexConsumer, boolean cull, Random random, long seed, int overlay) {
        boolean anyFacesRendered = false;
        float[] fs = new float[12];
        BitSet flags = new BitSet(3);
        ModelBlockRenderer.AmbientOcclusionFace ambientOcclusionCalculator = new ModelBlockRenderer.AmbientOcclusionFace();
        BlockPos.MutableBlockPos mutable = pos.mutable();
        final ModelBlockRendererInvoker invoker = (ModelBlockRendererInvoker) this;

        for (Direction direction : Direction.values()) {
            random.setSeed(seed);
            List<BakedQuad> faceQuads = model.getQuads(state, direction, random);

            mutable.setWithOffset(pos, direction);
            if (!faceQuads.isEmpty() && (!cull || shouldAlwaysDraw(direction) || Block.shouldRenderFace(state, level, pos, direction, mutable))) {
                invoker.worldmesher_renderQuadsSmooth(level, state, !shouldAlwaysDraw(direction) ? pos : pos.offset(0, 500, 0), poseStack, vertexConsumer, faceQuads, fs, flags, ambientOcclusionCalculator, overlay);
                anyFacesRendered = true;
            }
        }

        random.setSeed(seed);
        List<BakedQuad> quads = model.getQuads(state, null, random);
        if (!quads.isEmpty()) {
            invoker.worldmesher_renderQuadsSmooth(level, state, pos, poseStack, vertexConsumer, quads, fs, flags, ambientOcclusionCalculator, overlay);
            anyFacesRendered = true;
        }

        return anyFacesRendered;
    }

    @Override
    public boolean tesselateWithoutAO(BlockAndTintGetter level, BakedModel model, BlockState state, BlockPos pos, PoseStack poseStack, VertexConsumer vertexConsumer, boolean cull, Random random, long seed, int overlay) {
        boolean anyFacesRendered = false;
        BitSet flags = new BitSet(3);
        BlockPos.MutableBlockPos mutable = pos.mutable();
        final ModelBlockRendererInvoker invoker = (ModelBlockRendererInvoker) this;

        for (Direction direction : Direction.values()) {
            random.setSeed(seed);
            List<BakedQuad> faceQuads = model.getQuads(state, direction, random);

            mutable.setWithOffset(pos, direction);
            if (!faceQuads.isEmpty() && (!cull || shouldAlwaysDraw(direction) || Block.shouldRenderFace(state, level, pos, direction, mutable))) {
                int light = LevelRenderer.getLightColor(level, state, pos.relative(direction));
                invoker.worldmesher_renderQuadsFlat(level, state, !shouldAlwaysDraw(direction) ? pos : pos.offset(0, 500, 0), light, overlay, false, poseStack, vertexConsumer, faceQuads, flags);
                anyFacesRendered = true;
            }
        }


        random.setSeed(seed);
        List<BakedQuad> quads = model.getQuads(state, null, random);
        if (!quads.isEmpty()) {
            invoker.worldmesher_renderQuadsFlat(level, state, pos, -1, overlay, true, poseStack, vertexConsumer, quads, flags);
            anyFacesRendered = true;
        }

        return anyFacesRendered;
    }

    private boolean shouldAlwaysDraw(Direction direction) {
        return (cullingOverrides & (1 << List.of(Direction.values()).indexOf(direction))) != 0;
    }

}
