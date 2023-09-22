package io.wispforest.worldmesher.fabric;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.wispforest.worldmesher.IWorldMesherRenderContext;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.impl.client.indigo.renderer.IndigoRenderer;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.FabricWorldMesherRenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.BlockAndTintGetter;

import java.util.function.Function;

public class PlatformUtilsImpl {
    public static IWorldMesherRenderContext createContextOrNull(BlockAndTintGetter level, Function<RenderType, VertexConsumer> bufferFunc) {
        return RendererAccess.INSTANCE.getRenderer() instanceof IndigoRenderer ?
                new FabricWorldMesherRenderContext(level, bufferFunc) : null;
    }
}
