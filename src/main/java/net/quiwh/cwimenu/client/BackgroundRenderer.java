package net.quiwh.cwimenu.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class BackgroundRenderer {
    private static final ResourceLocation[] IMAGES = {
            ResourceLocation.fromNamespaceAndPath("cwi", "textures/gui/menu/background_1.png"),
            ResourceLocation.fromNamespaceAndPath("cwi", "textures/gui/menu/background_2.png")
    };
    private static SlideshowTimeline timeline;
    private static boolean ready;

    private BackgroundRenderer() {}

    public static void invalidate() {
        ready = false;
    }

    public static boolean isReady() {
        if (ready) return true;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getResourceManager() == null) return false;
        for (ResourceLocation image : IMAGES) {
            if (minecraft.getResourceManager().getResource(image).isEmpty()) return false;
        }
        ready = true;
        return true;
    }

    public static void render(GuiGraphics graphics, float opacity) {
        if (opacity <= 0 || !isReady()) return;
        long now = Util.getMillis();
        if (timeline == null) timeline = new SlideshowTimeline(now, ThreadLocalRandom.current().nextInt(2));
        SlideshowTimeline.Frame frame = timeline.frame(now);
        SlideshowTimeline.Blend blend = frame.blend(opacity);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (blend.current() > 0) draw(graphics, IMAGES[frame.current()], (float) blend.current());
        if (blend.previous() > 0) draw(graphics, IMAGES[frame.previous()], (float) blend.previous());
        graphics.setColor(1, 1, 1, 1);
    }

    private static void draw(GuiGraphics graphics, ResourceLocation image, float opacity) {
        graphics.setColor(1, 1, 1, opacity);
        graphics.blit(image, 0, 0, graphics.guiWidth(), graphics.guiHeight(), 0, 0, 2560, 1664, 2560, 1664);
    }
}
