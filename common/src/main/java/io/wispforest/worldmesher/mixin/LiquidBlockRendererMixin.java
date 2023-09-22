package io.wispforest.worldmesher.mixin;

import io.wispforest.worldmesher.renderers.IWorldMesherLiquidBlockRenderer;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mixin is injected before Fabric API to prevent it from caching
 * the WorldMesherLiquidBlockRenderer as the default instance
 */
@Mixin(value = LiquidBlockRenderer.class, priority = 900)
public class LiquidBlockRendererMixin {

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "setupSprites", at = @At("RETURN"), cancellable = true)
    private void cancelFabricOnTwitter(CallbackInfo ci) {
        if (!((Object) this instanceof IWorldMesherLiquidBlockRenderer)) return;
        ci.cancel();
    }

}
