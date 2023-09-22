package io.wispforest.worldmesher.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.wispforest.worldmesher.renderers.WorldMesherLiquidBlockRenderer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * This mixin is injected after Fabric API has done its things, so
 * that we can go right back and revert them if it's our renderer
 */
@Mixin(value = LiquidBlockRenderer.class, priority = 1100)
public class MixinLiquidBlockRendererMixin {

    @SuppressWarnings("MixinAnnotationTarget")
    @Shadow(remap = false)
    @Final
    private ThreadLocal<Boolean> fabric_customRendering;

    @SuppressWarnings({"CancellableInjectionUsage", "ConstantConditions"})
    @Inject(method = "tesselate", at = @At("HEAD"), cancellable = true, remap = false)
    private void cancelFabricOnTwitter(BlockAndTintGetter blockAndTintGetter, BlockPos blockPos, VertexConsumer vertexConsumer, BlockState blockState, FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof WorldMesherLiquidBlockRenderer)) return;
        fabric_customRendering.set(true);
        cir.cancel();
    }
}
