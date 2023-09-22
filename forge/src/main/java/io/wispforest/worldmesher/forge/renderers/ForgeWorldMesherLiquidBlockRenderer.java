package io.wispforest.worldmesher.forge.renderers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import io.wispforest.worldmesher.renderers.IWorldMesherLiquidBlockRenderer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;

public class ForgeWorldMesherLiquidBlockRenderer extends LiquidBlockRenderer implements IWorldMesherLiquidBlockRenderer {
    private Matrix4f matrix;

    public ForgeWorldMesherLiquidBlockRenderer(){
        setupSprites();
    }

    public void setMatrix(Matrix4f matrix) {
        this.matrix = matrix;
    }

    @Override
    public void vertexCall(VertexConsumer vertexConsumer, double x, double y, double z, float red, float green, float blue, float alpha, float u, float v, int light) {
        vertexConsumer.vertex(matrix, (float) x, (float) y, (float) z).color(red, green, blue, 1.0F).uv(u, v).uv2(light).normal(0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    protected void vertex(VertexConsumer vertexConsumer, double x, double y, double z, float red, float green, float blue, float alpha, float u, float v, int light) {
        this.vertexCall(vertexConsumer, x, y, z, red, blue, green, alpha, u, v, light);
    }
}
