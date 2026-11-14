package net.quiwh.cwimenu;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.quiwh.cwimenu.client.BackgroundRenderer;
import net.quiwh.cwimenu.client.CwiTitleScreen;
import net.quiwh.cwimenu.client.LastSession;
import net.quiwh.cwimenu.client.MenuMusic;
import org.slf4j.Logger;

@Mod(value = CwiMenu.ID, dist = Dist.CLIENT)
public final class CwiMenu {
    public static final String ID = "cwi_menu";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static boolean checkReloadedResources;
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ID);
    public static final DeferredHolder<SoundEvent, SoundEvent> MENU_SOUND = SOUNDS.register("menu",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ID, "menu")));

    public CwiMenu(IEventBus modBus) {
        SOUNDS.register(modBus);
        modBus.addListener(CwiMenu::reloadListeners);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CwiMenu::openScreen);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CwiMenu::initializeScreen);
        NeoForge.EVENT_BUS.addListener(CwiMenu::tick);
        NeoForge.EVENT_BUS.addListener(CwiMenu::loggedIn);
        LOGGER.info("CWI Menu loaded: native client presentation for Minecraft 1.21.1");
    }

    private static void reloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            BackgroundRenderer.invalidate();
            MenuMusic.onReload();
            checkReloadedResources = true;
        });
    }

    private static void openScreen(ScreenEvent.Opening event) {
        if (event.isCanceled()) return;
        if (event.getNewScreen() != null && event.getNewScreen().getClass() == TitleScreen.class
                && !Minecraft.getInstance().isDemo() && BackgroundRenderer.isReady()
                && CwiTitleScreen.resourcesAvailable()) {
            event.setNewScreen(new CwiTitleScreen());
        }
        if (event.getCurrentScreen() instanceof CwiTitleScreen && !(event.getNewScreen() instanceof CwiTitleScreen)) {
            CwiTitleScreen.onTitleLeft();
        }
        MenuMusic.screenOpening(event.getCurrentScreen(), event.getNewScreen());
    }

    private static void initializeScreen(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof CwiTitleScreen title) title.finishNativeInitialization();
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (checkReloadedResources && minecraft.getOverlay() == null) {
            checkReloadedResources = false;
            if (minecraft.screen instanceof CwiTitleScreen
                    && (!BackgroundRenderer.isReady() || !CwiTitleScreen.resourcesAvailable())) {
                LOGGER.warn("CWI menu artwork unavailable after reload; restoring the native title");
                minecraft.setScreen(new TitleScreen());
            }
        }
        MenuMusic.tick(minecraft);
    }

    private static void loggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        LastSession.rememberCurrent();
    }
}
