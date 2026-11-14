package net.quiwh.cwimenu.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

/** One native input/narration target owns its steel artwork and caption. */
public final class MenuButton extends Button {
    private final MenuLayout.Rect artwork;
    private final ResourceLocation normal;
    private final ResourceLocation hovered;
    private final int sourceSize;
    private final ButtonFeedback feedback = new ButtonFeedback();
    private MenuLayout.Rect captionRect;
    private Component caption;
    private Component help;
    private Tooltip reason;
    private boolean selected;
    private boolean ready;

    public MenuButton(MenuLayout layout, MenuLayout.Rect artwork, MenuLayout.Rect bounds,
                      Component message, ResourceLocation normal, ResourceLocation hovered,
                      int sourceSize, OnPress onPress) {
        super((int) Math.floor(layout.toScreenX(bounds.x())),
                (int) Math.floor(layout.toScreenY(bounds.y())),
                (int) Math.ceil(layout.toScreenX(bounds.x()+bounds.width())) - (int) Math.floor(layout.toScreenX(bounds.x())),
                (int) Math.ceil(layout.toScreenY(bounds.y()+bounds.height())) - (int) Math.floor(layout.toScreenY(bounds.y())),
                message, onPress, DEFAULT_NARRATION);
        this.artwork = artwork;
        this.normal = normal;
        this.hovered = hovered;
        this.sourceSize = sourceSize;
    }

    public void caption(Component text, MenuLayout.Rect rect) { caption = text; captionRect = rect; }
    public void help(Component text) { help = text; }

    /** Keep native disabled reasons in the fixed help slot and narration. */
    @Override
    public void setTooltip(Tooltip tooltip) {
        reason = tooltip;
        super.setTooltip(null);
    }

    public void prepare(boolean selected, boolean ready, long now, boolean immediate) {
        this.selected = selected;
        this.ready = ready;
        feedback.select(selected, now, immediate);
        if (!ready) feedback.release(false);
    }

    /** Raw bounds intentionally include disabled utilities so their reason remains discoverable. */
    public boolean containsPointer(double x, double y) {
        return visible && x >= getX() && y >= getY() && x < getRight() && y < getBottom();
    }

    public boolean showHelp(long now, boolean keyboard) { return ready && feedback.helpReady(now, keyboard); }

    public String helpText() {
        if (reason != null) {
            StringBuilder text = new StringBuilder();
            for (FormattedCharSequence line : reason.toCharSequence(Minecraft.getInstance())) {
                if (!text.isEmpty()) text.append(' ');
                line.accept((index, style, codepoint) -> { text.appendCodePoint(codepoint); return true; });
            }
            return text.toString();
        }
        return help == null ? "" : help.getString();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        if (normal == null) {
            drawFitted(graphics, font, getMessage(), artwork, .5f,
                    selected ? 0xE4ECE6 : 0x899698, alpha, false);
            return;
        }
        long now = Util.getMillis();
        float light = feedback.amount(now);
        boolean pressed = feedback.pressed() && containsPointer(mouseX, mouseY) && active;
        float pressBrightness = pressed ? .78f : 1;
        float hoverOpacity = active ? alpha * light : 0;
        // Preserve intro opacity while blending the two authored sprites.
        float normalOpacity = hoverOpacity >= 1 ? 0 : (alpha - hoverOpacity) / (1 - hoverOpacity);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        drawSprite(graphics, normal, 0, normalOpacity, (active ? .82f : .48f) * pressBrightness);
        drawSprite(graphics, hovered, 1, hoverOpacity, pressBrightness);
        graphics.setColor(1, 1, 1, 1);
        if (caption != null) {
            int text = active ? mix(0xBCC8C2, 0xEDF3EB, light) : 0x788580;
            drawFitted(graphics, font, Component.literal(caption.getString().strip()), captionRect, .75f, text, alpha, true);
        }
    }

    private void drawSprite(GuiGraphics graphics, ResourceLocation texture, int inset, float opacity, float brightness) {
        if (opacity <= 0) return;
        graphics.setColor(brightness, brightness, brightness, opacity);
        // Hover files have a transparent one-pixel rim; crop it to keep the panel aligned.
        int textureSize = sourceSize + inset * 2;
        graphics.blit(texture, artwork.x(), artwork.y(), artwork.width(), artwork.height(),
                inset, inset, sourceSize, sourceSize, textureSize, textureSize);
    }

    static void drawFitted(GuiGraphics graphics, Font font, Component text, MenuLayout.Rect rect,
                           float maximumScale, int rgb, float opacity, boolean center) {
        if (opacity < .016f) return;
        int width = font.width(text);
        float scale = Math.min(maximumScale, (rect.width()-2f)/Math.max(1, width));
        float x = center ? rect.x()+(rect.width()-width*scale)/2 : rect.x();
        graphics.pose().pushPose();
        graphics.pose().translate(x, rect.y(), 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, text, 0, 0, color(rgb, opacity), true);
        graphics.pose().popPose();
    }

    private static int color(int rgb, float opacity) { return Math.round(Math.clamp(opacity, 0, 1)*255)<<24 | rgb; }
    private static int mix(int from, int to, float t) {
        int value = 0;
        for (int shift : new int[]{16, 8, 0}) value |= Math.round(((from>>shift)&255)*(1-t)+((to>>shift)&255)*t)<<shift;
        return value;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button != 0 || !ready || !active || !containsPointer(x, y)) return false;
        feedback.press();
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button != 0) return false;
        boolean armed = feedback.pressed();
        if (feedback.release(ready && active && containsPointer(x, y))) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            onPress();
        }
        return armed;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return ready && super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void setFocused(boolean focused) {
        if (!focused) feedback.release(false);
        super.setFocused(focused);
    }

    @Override
    public void playDownSound(SoundManager sounds) {
        sounds.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), .85f, .18f));
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        super.updateWidgetNarration(output);
        if (reason != null) reason.updateNarration(output);
        else if (help != null) output.add(NarratedElementType.HINT, help);
    }
}
