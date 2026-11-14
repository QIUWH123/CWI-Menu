package net.quiwh.cwimenu.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.InputType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.internal.BrandingControl;
import net.quiwh.cwimenu.CwiMenu;

/** Native title lifecycle and actions presented with the original CWI composition. */
public final class CwiTitleScreen extends TitleScreen {
    private static final IntroTimeline INTRO = new IntroTimeline();
    private static final ResourceLocation LOGO = texture("title");
    private static final String[] REQUIRED_ARTWORK = {
            "title", "steel_l", "steel_l_hover", "last_world", "last_world_hover", "create", "create_hover",
            "resource", "resource_hover", "modlist", "modlist_hover", "accessibility", "accessibility_hover",
            "language", "language_hover"
    };
    private static final String[] CAPTIONS = {
            "menu.cwi.singleplayer", "menu.cwi.multiplayer", "menu.cwi.option", "menu.cwi.quit"
    };
    private final List<FadingButton> menuButtons = new ArrayList<>();
    private MenuLayout layout;
    private boolean initialFocusPending;
    private double lastMouseX = Double.NaN;
    private double lastMouseY = Double.NaN;

    public CwiTitleScreen() {
        super(false);
    }

    /** Keep the native title available when a resource pack removes required artwork. */
    public static boolean resourcesAvailable() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getResourceManager() == null) return false;
        for (String artwork : REQUIRED_ARTWORK) {
            if (minecraft.getResourceManager().getResource(texture(artwork)).isEmpty()) return false;
        }
        return true;
    }

    @Override
    protected void init() {
        // Ixeris and other title lifecycle hooks must observe the native initialization.
        super.init();
        LastSession.refresh();
        Map<String, Button> nativeButtons = new HashMap<>();
        for (GuiEventListener child : children()) {
            if (child instanceof Button button && button.getMessage().getContents() instanceof TranslatableContents text) {
                nativeButtons.putIfAbsent(text.getKey(), button);
            }
        }
        clearWidgets();
        menuButtons.clear();
        initialFocusPending = false;
        layout = MenuLayout.create(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(), width, height);

        boolean demo = minecraft.isDemo();
        String[] nativeKeys = {demo ? "menu.playdemo" : "menu.singleplayer", demo ? "menu.resetdemo" : "menu.multiplayer", "menu.options", "menu.quit"};
        for (int i = 0; i < nativeKeys.length; i++) {
            Button original = nativeButtons.get(nativeKeys[i]);
            if (original == null) continue;
            MenuButton button = new MenuButton(layout, layout.main(i), layout.mainBounds(i), original.getMessage(),
                    texture("steel_l"), texture("steel_l_hover"), 48, ignored -> { if (original.active) original.onPress(); });
            button.active = original.active;
            button.setTooltip(original.getTooltip());
            button.caption(Component.translatable(demo && i < 2 ? nativeKeys[i] : CAPTIONS[i]), layout.caption(i));
            addMenuButton(button, IntroTimeline.Part.MAIN);
        }

        MenuButton resume = addIcon(layout.utility(false, 0), "last_world", Component.translatable("cwi_menu.resume"),
                button -> { LastSession.resume(this); button.active = LastSession.canResume(); }, IntroTimeline.Part.UTILITY_TOP);
        resume.active = !demo && LastSession.canResume();
        if (!resume.active) resume.setTooltip(Tooltip.create(Component.translatable("cwi_menu.no_session")));
        resume.help(LastSession.description());

        Method createAction = findCreateAction();
        MenuButton create = addIcon(layout.utility(false, 1), "create", Component.translatable("cwi_menu.create"),
                button -> openCreate(createAction, button), IntroTimeline.Part.UTILITY_MIDDLE);
        create.active = createAction != null;
        create.help(Component.translatable("cwi_menu.help.create"));
        if (!create.active) create.setTooltip(Tooltip.create(Component.translatable("cwi_menu.create_unavailable")));

        addIcon(layout.utility(false, 2), "resource", Component.translatable("cwi_menu.reload"),
                button -> minecraft.reloadResourcePacks(), IntroTimeline.Part.UTILITY_BOTTOM)
                .help(Component.translatable("cwi_menu.help.reload"));
        help(addNative(nativeButtons.get("fml.menu.mods"), layout.utility(true, 0), "modlist", IntroTimeline.Part.UTILITY_TOP), "mods");
        help(addNative(nativeButtons.get("options.accessibility"), layout.utility(true, 1), "accessibility", IntroTimeline.Part.UTILITY_MIDDLE), "accessibility");
        help(addNative(nativeButtons.get("options.language"), layout.utility(true, 2), "language", IntroTimeline.Part.UTILITY_BOTTOM), "language");

        Button credits = nativeButtons.get("title.credits");
        if (credits != null) {
            MenuButton button = new MenuButton(layout, layout.copyright(), layout.copyright(), credits.getMessage(),
                    null, null, 0, ignored -> credits.onPress());
            button.active = credits.active;
            button.setTooltip(credits.getTooltip());
            addMenuButton(button, null);
        }
        updateButtonFades(Util.getMillis());
    }

    /** Remove automatic title decorations appended after our native init returns. */
    public void finishNativeInitialization() {
        if (layout == null) return;
        List<GuiEventListener> extras = new ArrayList<>(children());
        for (FadingButton entry : menuButtons) extras.remove(entry.button());
        for (GuiEventListener extra : extras) removeWidget(extra);
    }

    /** Called by the final screen-opening event, never by a resize rebuild. */
    public static void onTitleLeft() {
        INTRO.leave();
    }

    private MenuButton addNative(Button original, MenuLayout.Rect rect, String artwork, IntroTimeline.Part part) {
        if (original == null) return null;
        MenuButton button = addIcon(rect, artwork, original.getMessage(), ignored -> {
            if (original.active) original.onPress();
        }, part);
        button.active = original.active;
        button.setTooltip(original.getTooltip());
        return button;
    }

    private static void help(MenuButton button, String name) {
        if (button != null) button.help(Component.translatable("cwi_menu.help." + name));
    }

    private MenuButton addIcon(MenuLayout.Rect rect, String artwork, Component label, Button.OnPress action, IntroTimeline.Part part) {
        MenuButton button = new MenuButton(layout, rect, rect, label, texture(artwork), texture(artwork + "_hover"), 22, action);
        addMenuButton(button, part);
        return button;
    }

    private void addMenuButton(MenuButton button, IntroTimeline.Part part) {
        addRenderableWidget(button);
        menuButtons.add(new FadingButton(button, part));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (layout == null) return;
        long now = Util.getMillis();
        if (Double.isNaN(lastMouseX)) { lastMouseX = mouseX; lastMouseY = mouseY; }
        if (minecraft.getOverlay() == null) INTRO.enter(now);
        boolean immediate = minecraft.options.screenEffectScale().get() == 0;
        if (immediate) INTRO.leave();
        updateButtonFades(now);
        boolean ready = INTRO.isInteractive(now) && minecraft.getOverlay() == null;
        boolean keyboard = minecraft.getLastInputType().isKeyboard();
        MenuButton selected = null;
        for (FadingButton entry : menuButtons) {
            MenuButton button = entry.button();
            if (keyboard ? button.isFocused() : button.containsPointer(mouseX, mouseY)) { selected = button; break; }
        }
        for (FadingButton entry : menuButtons) entry.button().prepare(entry.button() == selected, ready, now, immediate);
        BackgroundRenderer.render(graphics, 1);
        graphics.pose().pushPose();
        graphics.pose().scale((float) layout.scaleX(), (float) layout.scaleY(), 1);
        MenuLayout.Rect band = layout.band();
        graphics.fill(band.x(), band.y(), band.x() + band.width(), band.y() + band.height(),
                Math.round(INTRO.alpha(IntroTimeline.Part.BAND, now) * 255) << 24);
        drawLogo(graphics, INTRO.alpha(IntroTimeline.Part.LOGO, now));
        for (FadingButton entry : menuButtons) entry.button().render(graphics, mouseX, mouseY, partialTick);
        if (selected != null && selected.showHelp(now, keyboard)) drawHelp(graphics, selected.helpText());
        graphics.pose().pushPose();
        graphics.pose().translate(0, layout.height(), 0);
        graphics.pose().scale(.5f, .5f, 1);
        BrandingControl.forEachLine(true, true, (line, text) ->
                graphics.drawString(font, text, 8, -(12 + line * (font.lineHeight + 1)), 0xFF899698));
        BrandingControl.forEachAboveCopyrightLine((line, text) ->
                graphics.drawString(font, text, layout.width()*2 - font.width(text)-8,
                        -(12 + (line + 1) * (font.lineHeight + 1)), 0xFF899698));
        graphics.pose().popPose();
        graphics.pose().popPose();
        graphics.setColor(1, 1, 1, 1);
    }

    private void drawLogo(GuiGraphics graphics, float opacity) {
        if (opacity <= 0) return;
        MenuLayout.Rect rect = layout.logo();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1, 1, 1, opacity);
        graphics.blit(LOGO, rect.x(), rect.y(), rect.width(), rect.height(), 0, 0, 584, 148, 584, 148);
        graphics.setColor(1, 1, 1, 1);
    }

    private void drawHelp(GuiGraphics graphics, String text) {
        if (text.isBlank()) return;
        MenuLayout.Rect rect = layout.help();
        int maxWidth = (int) ((rect.width()-4)/.65f);
        if (font.width(text) > maxWidth) text = font.plainSubstrByWidth(text, maxWidth-font.width("...")) + "...";
        MenuButton.drawFitted(graphics, font, Component.literal(text), rect, .65f, 0xA1B9B1, 1, true);
    }

    private void updateButtonFades(long now) {
        boolean mainVisible = false;
        for (FadingButton entry : menuButtons) {
            float opacity = entry.part() == null ? 1 : INTRO.alpha(entry.part(), now);
            entry.button().setAlpha(opacity);
            entry.button().visible = opacity > 0;
            if (entry.part() == IntroTimeline.Part.MAIN && INTRO.isInteractive(now)) mainVisible = true;
        }
        if (initialFocusPending && mainVisible) setInitialFocus();
    }

    @Override
    protected void setInitialFocus() {
        if (getFocused() != null) {
            initialFocusPending = false;
            return;
        }
        for (FadingButton entry : menuButtons) {
            if (entry.part() == IntroTimeline.Part.MAIN && INTRO.isInteractive(Util.getMillis())) {
                initialFocusPending = false;
                super.setInitialFocus();
                return;
            }
        }
        // The native init hook runs before the first-entry main-button delay ends.
        initialFocusPending = true;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        minecraft.setLastInputType(InputType.MOUSE);
        // TitleScreen's fallback targets hidden Realms notifications; use our visible children only.
        for (GuiEventListener child : children()) {
            if (child.mouseClicked(x, y, button)) {
                if (getFocused() != child) setFocused(child);
                if (button == 0) setDragging(true);
                return true;
            }
        }
        return false;
    }

    @Override
    public void mouseMoved(double x, double y) {
        if (!Double.isNaN(lastMouseX) && (x != lastMouseX || y != lastMouseY)) minecraft.setLastInputType(InputType.MOUSE);
        lastMouseX = x;
        lastMouseY = y;
        super.mouseMoved(x, y);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        boolean handled = getFocused() != null && getFocused().mouseReleased(x, y, button);
        setDragging(false);
        return handled;
    }

    private static Method findCreateAction() {
        try {
            return Class.forName("com.simibubi.create.infrastructure.gui.OpenCreateMenuButton").getMethod("click", Button.class);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private static void openCreate(Method action, Button button) {
        if (action == null) return;
        try {
            action.invoke(null, button);
        } catch (IllegalAccessException | InvocationTargetException | LinkageError exception) {
            CwiMenu.LOGGER.warn("Create menu integration is unavailable", exception);
            button.active = false;
            button.setTooltip(Tooltip.create(Component.translatable("cwi_menu.create_unavailable")));
        }
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("cwi", "textures/gui/menu/" + name + ".png");
    }

    private record FadingButton(MenuButton button, IntroTimeline.Part part) {}
}
