package net.quiwh.cwimenu.mixin;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.quiwh.cwimenu.client.BackgroundRenderer;
import net.quiwh.cwimenu.client.LoadingPainter;
import net.quiwh.cwimenu.client.LoadingVisualState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces draw calls only; all native reload and screen lifecycle code still runs. */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private boolean fadeIn;
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart;
    @Shadow private long fadeInStart;
    @Unique private boolean cwi$replaceVisuals;

    @Shadow
    private void drawProgressBar(GuiGraphics graphics, int left, int top, int right, int bottom, float opacity) {
        throw new AssertionError("Mixin shadow");
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void cwi$chooseVisuals(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        cwi$replaceVisuals = minecraft.getOverlay() == (Object)this && BackgroundRenderer.isReady();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(Lnet/minecraft/client/renderer/RenderType;IIIII)V"), require = 2)
    private void cwi$nativeBackground(GuiGraphics graphics, RenderType type,
                                      int left, int top, int right, int bottom, int color) {
        if (!cwi$replaceVisuals) graphics.fill(type, left, top, right, bottom, color);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V"), require = 2)
    private void cwi$nativeLogo(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height,
                                float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        if (!cwi$replaceVisuals) graphics.blit(texture, x, y, width, height, u, v,
                regionWidth, regionHeight, textureWidth, textureHeight);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/LoadingOverlay;drawProgressBar(Lnet/minecraft/client/gui/GuiGraphics;IIIIF)V"), require = 1)
    private void cwi$nativeBar(LoadingOverlay overlay, GuiGraphics graphics,
                              int left, int top, int right, int bottom, float opacity) {
        if (!cwi$replaceVisuals) drawProgressBar(graphics, left, top, right, bottom, opacity);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void cwi$paintVisuals(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (cwi$replaceVisuals && minecraft.getOverlay() == (Object)this) {
            LoadingPainter.render(graphics, currentProgress,
                    LoadingVisualState.reload(Util.getMillis(), fadeIn, fadeInStart, fadeOutStart));
        }
    }
}
