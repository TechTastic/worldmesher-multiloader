package io.wispforest.worldmesher;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import io.wispforest.worldmesher.renderers.WorldMesherBlockModelRenderer;
import io.wispforest.worldmesher.renderers.WorldMesherLiquidBlockRenderer;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class WorldMesh {
    private final Level level;
    private final BlockPos origin;
    private final BlockPos end;
    private final boolean cull;
    private final AABB dimensions;

    private MeshState state = MeshState.NEW;
    private float buildProgress = 0;

    private final Map<RenderType, VertexBuffer> bufferStorage;
    private final Map<RenderType, BufferBuilder> initializedLayers;

    private DynamicRenderInfo renderInfo;
    private boolean entitiesFrozen;
    private boolean freezeEntities;

    private final Runnable renderStartAction;
    private final Runnable renderEndAction;

    private Supplier<PoseStack> matrixStackSupplier = PoseStack::new;

    private WorldMesh(Level level, BlockPos origin, BlockPos end, boolean cull, boolean freezeEntities, Runnable renderStartAction, Runnable renderEndAction) {
        this.level = level;
        this.origin = origin;
        this.end = end;

        this.cull = cull;
        this.freezeEntities = freezeEntities;
        this.dimensions = new AABB(this.origin, this.end);

        this.renderStartAction = renderStartAction;
        this.renderEndAction = renderEndAction;

        this.bufferStorage = new HashMap<>();
        this.initializedLayers = new HashMap<>();
        this.renderInfo = new DynamicRenderInfo();

        this.scheduleRebuild();
    }

    /**
     * Renders this level mesh into the current framebuffer, translated using the given matrix
     *
     * @param matrices The translation matrices. This is applied to the entire mesh
     */
    public void render(PoseStack matrices) {

        final var matrix = matrices.last().pose();

        if (!this.canRender()) {
            throw new IllegalStateException("World mesh not prepared!");
        }

        final RenderType translucent = RenderType.translucent();
        bufferStorage.forEach((renderLayer, vertexBuffer) -> {
            if (renderLayer == translucent) return;
            draw(renderLayer, vertexBuffer, matrix);
        });

        if (bufferStorage.containsKey(translucent)) {
            draw(translucent, bufferStorage.get(translucent), matrix);
        }

        if (!bufferStorage.isEmpty()) VertexBuffer.unbind();
    }

    public void setMatrixStackSupplier(Supplier<PoseStack> stackSupplier) {
        this.matrixStackSupplier = stackSupplier;
    }

    /**
     * Checks whether this mesh is ready for rendering
     */
    public boolean canRender() {
        return this.state.canRender;
    }

    /**
     * Returns the current state of this mesh, used to indicate building progress and rendering availability
     *
     * @return The current {@code MeshState} constant
     */
    public MeshState getState() {
        return this.state;
    }

    /**
     * How much of this mesh is built
     *
     * @return The build progress of this mesh
     */
    public float getBuildProgress() {
        return this.buildProgress;
    }

    public DynamicRenderInfo getRenderInfo() {
        return this.renderInfo;
    }

    public BlockPos startPos() {
        return this.origin;
    }

    public BlockPos endPos() {
        return this.end;
    }

    public boolean entitiesFrozen() {
        return this.entitiesFrozen;
    }

    public void setFreezeEntities(boolean freezeEntities) {
        this.freezeEntities = freezeEntities;
    }

    public AABB dimensions() {
        return dimensions;
    }

    /**
     * Schedules a rebuild of this mesh
     */
    public void scheduleRebuild() {
        if (state.isBuildStage) return;
        state = state == MeshState.NEW ? MeshState.BUILDING : MeshState.REBUILDING;
        initializedLayers.clear();

        CompletableFuture.runAsync(this::build, Util.backgroundExecutor()).whenComplete((unused, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                state = MeshState.NEW;
            } else {
                state = MeshState.READY;
            }
        });
    }

    private void build() {
        final var client = Minecraft.getInstance();
        final var blockRenderManager = client.getBlockRenderer();
        final var blockRenderer = new WorldMesherBlockModelRenderer();
        final var fluidRenderer = new WorldMesherLiquidBlockRenderer();
        PoseStack matrices = matrixStackSupplier.get();

        // TODO: IndigoRenderer Fuckery
        Random random = new Random();
        var renderContext = PlatformUtils.createContextOrNull(this.level, this::getOrCreateBuffer);

        List<BlockPos> possess = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(this.origin, this.end)) {
            possess.add(pos.immutable());
        }

        int blocksToBuild = possess.size();
        final DynamicRenderInfo.Mutable tempRenderInfo = new DynamicRenderInfo.Mutable();

        this.entitiesFrozen = this.freezeEntities;
        final var entitiesFuture = new CompletableFuture<List<DynamicRenderInfo.EntityEntry>>();
        client.execute(() -> {
            entitiesFuture.complete(
                    client.level.getEntities(client.player, new AABB(this.origin, this.end).inflate(.5), entity -> !(entity instanceof Player))
                            .stream()
                            .map(entity -> {
                                if (freezeEntities) {
                                    var originalEntity = entity;
                                    entity = entity.getType().create(client.level);

                                    entity.restoreFrom(originalEntity);
                                    entity.copyPosition(originalEntity);
                                    //entity.rotate(originalEntity.get)
                                    entity.tick();
                                }

                                return new DynamicRenderInfo.EntityEntry(
                                        entity,
                                        client.getEntityRenderDispatcher().getPackedLightCoords(entity, 0)
                                );
                            }).toList()
            );
        });

        for (int i = 0; i < blocksToBuild; i++) {
            BlockPos pos = possess.get(i);
            BlockPos renderPos = pos.subtract(origin);

            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;

            if (level.getBlockEntity(pos) != null) tempRenderInfo.addBlockEntity(renderPos, level.getBlockEntity(pos));

            if (!level.getFluidState(pos).isEmpty()) {

                FluidState fluidState = level.getFluidState(pos);
                RenderType fluidLayer = null;//TODO: RenderType.getFluidLayer(fluidState);

                matrices.pushPose();
                matrices.translate(-(pos.getX() & 15), -(pos.getY() & 15), -(pos.getZ() & 15));
                matrices.translate(renderPos.getX(), renderPos.getY(), renderPos.getZ());

                fluidRenderer.setMatrix(matrices.last().pose());
                fluidRenderer.tesselate(level, pos, getOrCreateBuffer(fluidLayer), state, fluidState);

                matrices.popPose();
            }

            matrices.pushPose();
            matrices.translate(renderPos.getX(), renderPos.getY(), renderPos.getZ());

            blockRenderer.clearCullingOverrides();
            blockRenderer.setCullDirection(Direction.EAST, pos.getX() == this.end.getX());
            blockRenderer.setCullDirection(Direction.WEST, pos.getX() == this.origin.getX());
            blockRenderer.setCullDirection(Direction.SOUTH, pos.getZ() == this.end.getZ());
            blockRenderer.setCullDirection(Direction.NORTH, pos.getZ() == this.origin.getZ());
            blockRenderer.setCullDirection(Direction.UP, pos.getY() == this.end.getY());
            blockRenderer.setCullDirection(Direction.DOWN, pos.getY() == this.origin.getY());

            RenderType renderLayer = null; //TODO: RenderLayers.getBlockLayer(state);

            final var model = blockRenderManager.getBlockModel(state);
            if (model.isCustomRenderer() && renderContext != null)
                renderContext.tessellateBlock(this.level, state, pos, model, matrices);
            else if (state.getRenderShape() == RenderShape.MODEL) {
                blockRenderer.tesselateBlock(this.level, model, state, pos, matrices, getOrCreateBuffer(renderLayer), cull, random, state.getSeed(pos), OverlayTexture.NO_OVERLAY);
            }

            matrices.popPose();

            this.buildProgress = i / (float) blocksToBuild;
        }

        if (initializedLayers.containsKey(RenderType.translucent())) {
            var translucentBuilder = initializedLayers.get(RenderType.translucent());
            var camera = client.gameRenderer.getMainCamera();
            translucentBuilder.setQuadSortOrigin((float) camera.getBlockPosition().getX() - (float) origin.getX(), (float) camera.getBlockPosition().getY() - (float) origin.getY(), (float) camera.getBlockPosition().getZ() - (float) origin.getZ());
        }

        initializedLayers.values().forEach(BufferBuilder::end);

        List<CompletableFuture<Void>> list = Lists.newArrayList();
        initializedLayers.forEach((renderLayer, bufferBuilder) -> {
            final var vertexBuffer = new VertexBuffer();
            bufferStorage.put(renderLayer, vertexBuffer);
            list.add(bufferStorage.get(renderLayer).uploadLater(bufferBuilder));
        });
        Util.sequence(list).handle((voids, throwable) -> {
            if (throwable != null) {
                CrashReport crashReport = CrashReport.forThrowable(throwable, "Building WorldMesher Mesh");
                Minecraft.getInstance().delayCrash(() -> Minecraft.getInstance().fillReport(crashReport));
            }
            return true;
        }).join();

        entitiesFuture.join().forEach(entry -> tempRenderInfo.addEntity(entry.entity().position().subtract(origin.getX(), origin.getY(), origin.getZ()), entry));
        this.renderInfo = tempRenderInfo.toImmutable();
    }

    private VertexConsumer getOrCreateBuffer(RenderType layer) {
        if (!initializedLayers.containsKey(layer)) {
            BufferBuilder builder = new BufferBuilder(layer.bufferSize());
            initializedLayers.put(layer, builder);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        }

        return initializedLayers.get(layer);
    }

    private void draw(RenderType renderLayer, VertexBuffer vertexBuffer, Matrix4f matrix) {
        renderLayer.setupRenderState();
        renderStartAction.run();

        vertexBuffer._drawWithShader(matrix, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());

        renderEndAction.run();
        renderLayer.clearRenderState();
    }

    public static class Builder {

        private final Level level;

        private final BlockPos origin;
        private final BlockPos end;
        private boolean cull = true;
        private boolean freezeEntities = false;

        private Runnable startAction = () -> {};
        private Runnable endAction = () -> {};

        public Builder(Level level, BlockPos origin, BlockPos end) {
            this.level = level;
            this.origin = origin;
            this.end = end;
        }

        public Builder disableCulling() {
            this.cull = false;
            return this;
        }

        public Builder freezeEntities() {
            this.freezeEntities = true;
            return this;
        }

        public Builder renderActions(Runnable startAction, Runnable endAction) {
            this.startAction = startAction;
            this.endAction = endAction;
            return this;
        }

        public WorldMesh build() {
            BlockPos start = new BlockPos(Math.min(origin.getX(), end.getX()), Math.min(origin.getY(), end.getY()), Math.min(origin.getZ(), end.getZ()));
            BlockPos target = new BlockPos(Math.max(origin.getX(), end.getX()), Math.max(origin.getY(), end.getY()), Math.max(origin.getZ(), end.getZ()));

            return new WorldMesh(level, start, target, cull, freezeEntities, startAction, endAction);
        }
    }

    public enum MeshState {
        NEW(false, false),
        BUILDING(true, false),
        REBUILDING(true, true),
        READY(false, true);

        public final boolean isBuildStage;
        public final boolean canRender;

        MeshState(boolean buildStage, boolean canRender) {
            this.isBuildStage = buildStage;
            this.canRender = canRender;
        }
    }
}
