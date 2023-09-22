package io.wispforest.worldmesher.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.BitSet;
import java.util.List;

@Mixin(ModelBlockRenderer.class)
public interface ModelBlockRendererInvoker {

    @Invoker("renderModelFaceAO")
    void worldmesher_renderQuadsSmooth(BlockAndTintGetter level, BlockState state, BlockPos pos, PoseStack poseStack, VertexConsumer vertexConsumer, List<BakedQuad> quads, float[] box, BitSet flags, ModelBlockRenderer.AmbientOcclusionFace ambientOcclusionCalculator, int overlay);

    @Invoker("renderModelFaceFlat")
    void worldmesher_renderQuadsFlat(BlockAndTintGetter level, BlockState state, BlockPos pos, int light, int overlay, boolean useWorldLight, PoseStack poseStack, VertexConsumer vertexConsumer, List<BakedQuad> quads, BitSet flags);

}
