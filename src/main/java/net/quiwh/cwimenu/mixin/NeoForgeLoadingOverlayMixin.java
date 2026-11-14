package net.quiwh.cwimenu.mixin;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.loading.NeoForgeLoadingOverlay;
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

/** NeoForge owns separate progress/fade fields and overrides the entire render method. */
@Mixin(value = NeoForgeLoadingOverlay.class, remap = false)
public abstract class NeoForgeLoadingOverlayMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private float currentProgress;
    @Shadow private long fadeOutStart;
    @Unique private boolean cwi$replaceVisuals;

    @Inject(method = "render", at = @At("HEAD"))
    private void cwi$chooseVisuals(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        cwi$replaceVisuals = minecraft.getOverlay() == (Object)this && BackgroundRenderer.isReady();
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V"), require = 2)
    private void cwi$nativeFramebuffer(MeshData mesh) {
        if (cwi$replaceVisuals) {
            // Building the mesh has already acquired native buffers; always release
            // them even though this visual pass does not submit them to the GPU.
            mesh.close();
        } else {
            BufferUploader.drawWithShader(mesh);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void cwi$paintVisuals(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (cwi$replaceVisuals && minecraft.getOverlay() == (Object)this) {
            LoadingPainter.render(graphics, currentProgress,
                    LoadingVisualState.startup(Util.getMillis(), fadeOutStart));
        }
    }
}
