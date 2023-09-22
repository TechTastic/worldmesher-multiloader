package net.fabricmc.fabric.impl.client.indigo.renderer.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import io.wispforest.worldmesher.IWorldMesherRenderContext;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoLuminanceFix;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;
import java.util.function.Function;

public class FabricWorldMesherRenderContext extends AbstractRenderContext implements IWorldMesherRenderContext {
    private final BlockRenderInfo blockInfo;
    private final AoCalculator aoCalc;

    private final AbstractMeshConsumer meshConsumer;
    private final TerrainFallbackConsumer fallbackConsumer;

    public FabricWorldMesherRenderContext(BlockAndTintGetter level, Function<RenderType, VertexConsumer> bufferFunc) {
        this.blockInfo = new BlockRenderInfo();
        this.blockInfo.prepareForWorld(level, true);

        this.aoCalc = new AoCalculator(blockInfo) {
            @Override
            public int light(BlockPos pos, BlockState state) {
                return LevelRenderer.getLightColor(level, state, pos);
            }

            @Override
            public float ao(BlockPos pos, BlockState state) {
                return AoLuminanceFix.INSTANCE.apply(level, pos, state);
            }
        };

        this.meshConsumer = new AbstractMeshConsumer(blockInfo, bufferFunc, aoCalc, this::transform) {
            @Override
            protected int overlay() {
                return overlay;
            }

            @Override
            protected Matrix4f matrix() {
                return matrix;
            }

            @Override
            protected Matrix3f normalMatrix() {
                return normalMatrix;
            }
        };

        this.fallbackConsumer = new TerrainFallbackConsumer(blockInfo, bufferFunc, aoCalc, this::transform) {
            @Override
            protected int overlay() {
                return overlay;
            }

            @Override
            protected Matrix4f matrix() {
                return matrix;
            }

            @Override
            protected Matrix3f normalMatrix() {
                return normalMatrix;
            }
        };
    }

    @Override
    public void tessellateBlock(BlockAndTintGetter level, BlockState state, BlockPos pos, BakedModel model, PoseStack poseStack) {
        this.matrix = poseStack.last().pose();
        this.normalMatrix = poseStack.last().normal();

        try {
            aoCalc.clear();
            blockInfo.prepareForBlock(state, pos, model.useAmbientOcclusion());
            ((FabricBakedModel) model).emitBlockQuads(blockInfo.blockView, blockInfo.blockState, blockInfo.blockPos, blockInfo.randomSupplier, this);
        } catch (Throwable throwable) {
            CrashReport crashReport = CrashReport.forThrowable(throwable, "Tessellating block in WorldMesher mesh");
            CrashReportCategory crashReportSection = crashReport.addCategory("Block being tessellated");
            CrashReportCategory.populateBlockDetails(crashReportSection, level, pos, state);
            try {
                throw crashReport.getException();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public Consumer<Mesh> meshConsumer() {
        return this.meshConsumer;
    }

    @Override
    public Consumer<BakedModel> fallbackConsumer() {
        return this.fallbackConsumer;
    }

    @Override
    public QuadEmitter getEmitter() {
        return this.meshConsumer.getEmitter();
    }
}
