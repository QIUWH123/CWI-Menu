package net.quiwh.cwimenu.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.nio.IntBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.GlStateBackup;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.system.MemoryStack;

/** A visuals-only pass shared by vanilla reload and NeoForge startup overlays. */
public final class LoadingPainter {
    private static final ResourceLocation MOJANG_LOGO =
            ResourceLocation.withDefaultNamespace("textures/gui/title/mojangstudios.png");

    private LoadingPainter() {}

    public static void render(GuiGraphics graphics, float progress, LoadingVisualState state) {
        if (state.backgroundAlpha() <= 0 && state.elementsAlpha() <= 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (width <= 0 || height <= 0) return;

        // NeoForge deliberately leaves its early framebuffer projection installed.
        // Keep our own copies rather than using RenderSystem's singleton backup slot.
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4f textureMatrix = new Matrix4f(RenderSystem.getTextureMatrix());
        VertexSorting sorting = RenderSystem.getVertexSorting();
        ShaderInstance shader = RenderSystem.getShader();
        int shaderTexture = RenderSystem.getShaderTexture(0);
        int activeTexture = GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE);
        float[] color = RenderSystem.getShaderColor().clone();
        GlStateBackup glState = new GlStateBackup();
        RenderSystem.backupGlState(glState);

        try (MemoryStack memory = MemoryStack.stackPush()) {
            IntBuffer viewport = memory.mallocInt(4);
            GL11C.glGetIntegerv(GL11C.GL_VIEWPORT, viewport);
            graphics.pose().pushPose();
            try {
                graphics.flush();
                graphics.pose().setIdentity();
                float far = ClientHooks.getGuiFarPlane();
                RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, width, height, 0, 1000, far),
                        VertexSorting.ORTHOGRAPHIC_Z);
                RenderSystem.getModelViewMatrix().translation(0, 0, 10000 - far);
                RenderSystem.resetTextureMatrix();
                RenderSystem.viewport(0, 0, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
                RenderSystem.disableScissor();
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.disableCull();
                RenderSystem.disableColorLogicOp();
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                graphics.setColor(1, 1, 1, 1);
                BackgroundRenderer.render(graphics, state.backgroundAlpha());
                graphics.flush();

                LoadingVisualState.Geometry geometry = LoadingVisualState.geometry(width, height);
                if (state.elementsAlpha() > 0) {
                    drawMojangLogo(graphics, geometry, state.elementsAlpha());
                    drawProgressBar(graphics, geometry, progress, state.elementsAlpha());
                }
            } finally {
                // Submit buffered rectangles while our projection is still active.
                try {
                    graphics.flush();
                } finally {
                    graphics.pose().popPose();
                    RenderSystem.setProjectionMatrix(projection, sorting);
                    RenderSystem.getModelViewMatrix().set(modelView);
                    RenderSystem.setTextureMatrix(textureMatrix);
                    RenderSystem.setShader(() -> shader);
                    RenderSystem.setShaderTexture(0, shaderTexture);
                    RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
                    RenderSystem.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                    RenderSystem.restoreGlState(glState);
                    GlStateManager._activeTexture(activeTexture);
                }
            }
        }
    }

    private static void drawMojangLogo(GuiGraphics graphics, LoadingVisualState.Geometry g, float opacity) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        // Native LogoTexture contains black surrounding pixels; the native additive
        // blend retains only the white mark without introducing a black rectangle.
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        graphics.setColor(1, 1, 1, opacity);
        graphics.blit(MOJANG_LOGO, g.left(), g.logoTop(), g.halfWidth(), g.logoHeight(),
                -.0625f, 0, 120, 60, 120, 120);
        graphics.blit(MOJANG_LOGO, g.centerX(), g.logoTop(), g.halfWidth(), g.logoHeight(),
                .0625f, 60, 120, 60, 120, 120);
        graphics.setColor(1, 1, 1, 1);
        RenderSystem.defaultBlendFunc();
    }

    private static void drawProgressBar(GuiGraphics graphics, LoadingVisualState.Geometry g,
                                       float progress, float opacity) {
        int left = g.left();
        int right = g.right();
        int top = g.barTop();
        int bottom = top + 10;
        int color = Math.round(opacity * 255) << 24 | 0xFFFFFF;
        graphics.fill(left + 2, top + 2, left + LoadingVisualState.progressPixels(g, progress), bottom - 2, color);
        graphics.fill(left + 1, top, right - 1, top + 1, color);
        graphics.fill(left + 1, bottom, right - 1, bottom - 1, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right, top, right - 1, bottom, color);
    }
}
