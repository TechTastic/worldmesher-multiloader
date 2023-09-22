package io.wispforest.worldmesher;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.architectury.injectables.annotations.ExpectPlatform;
import io.wispforest.worldmesher.renderers.IWorldMesherLiquidBlockRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.BlockAndTintGetter;

import java.util.function.Function;

public class PlatformUtils {
    @ExpectPlatform
    public static IWorldMesherRenderContext createContextOrNull(BlockAndTintGetter level, Function<RenderType, VertexConsumer> bufferFunc) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static IWorldMesherLiquidBlockRenderer createLiquidBlockRenderer() {
        throw new AssertionError();
    }
}
