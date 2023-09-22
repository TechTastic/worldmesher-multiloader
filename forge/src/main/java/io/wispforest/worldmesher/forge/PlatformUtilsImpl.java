package io.wispforest.worldmesher.forge;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.wispforest.worldmesher.IWorldMesherRenderContext;
import io.wispforest.worldmesher.forge.renderers.ForgeWorldMesherLiquidBlockRenderer;
import io.wispforest.worldmesher.renderers.IWorldMesherLiquidBlockRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.BlockAndTintGetter;

import java.util.function.Function;

public class PlatformUtilsImpl {
    public static IWorldMesherRenderContext createContextOrNull(BlockAndTintGetter level, Function<RenderType, VertexConsumer> bufferFunc) {
        return null;
    }

    public static IWorldMesherLiquidBlockRenderer createLiquidBlockRenderer() {
        return new ForgeWorldMesherLiquidBlockRenderer();
    }
}
