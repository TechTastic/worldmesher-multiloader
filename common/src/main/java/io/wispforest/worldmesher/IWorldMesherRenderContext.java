package io.wispforest.worldmesher;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public interface IWorldMesherRenderContext {
    void tessellateBlock(BlockAndTintGetter level, BlockState state, BlockPos pos, final BakedModel model, PoseStack poseStack);
}
