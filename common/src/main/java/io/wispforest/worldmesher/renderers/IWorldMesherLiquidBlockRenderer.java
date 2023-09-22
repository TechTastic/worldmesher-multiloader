package io.wispforest.worldmesher.renderers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;

public interface IWorldMesherLiquidBlockRenderer {

    void setMatrix(Matrix4f matrix);

    void vertexCall(VertexConsumer vertexConsumer, double x, double y, double z, float red, float green, float blue, float alpha, float u, float v, int light);
}
