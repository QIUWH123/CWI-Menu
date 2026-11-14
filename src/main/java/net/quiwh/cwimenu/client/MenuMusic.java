package net.quiwh.cwimenu.client;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.quiwh.cwimenu.CwiMenu;

public final class MenuMusic {
    private static final MenuMusicPolicy POLICY = new MenuMusicPolicy();
    private static final Set<String> MENU_CHILDREN = Set.of(
            "net.minecraft.client.gui.screens.options.OptionsScreen",
            "net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen",
            "net.minecraft.client.gui.screens.options.ChatOptionsScreen",
            "net.minecraft.client.gui.screens.options.FontOptionsScreen",
            "net.minecraft.client.gui.screens.options.LanguageSelectScreen",
            "net.minecraft.client.gui.screens.options.MouseSettingsScreen",
            "net.minecraft.client.gui.screens.options.OnlineOptionsScreen",
            "net.minecraft.client.gui.screens.options.SkinCustomizationScreen",
            "net.minecraft.client.gui.screens.options.SoundOptionsScreen",
            "net.minecraft.client.gui.screens.options.VideoSettingsScreen",
            "net.minecraft.client.gui.screens.options.UnsupportedGraphicsWarningScreen",
            "net.minecraft.client.gui.screens.options.controls.ControlsScreen",
            "net.minecraft.client.gui.screens.options.controls.KeyBindsScreen",
            "net.minecraft.client.gui.screens.worldselection.SelectWorldScreen",
            "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen",
            "net.minecraft.client.gui.screens.worldselection.EditWorldScreen",
            "net.minecraft.client.gui.screens.worldselection.EditGameRulesScreen",
            "net.minecraft.client.gui.screens.worldselection.ExperimentsScreen",
            "net.minecraft.client.gui.screens.worldselection.ConfirmExperimentalFeaturesScreen",
            "net.minecraft.client.gui.screens.CreateBuffetWorldScreen",
            "net.minecraft.client.gui.screens.CreateFlatWorldScreen",
            "net.minecraft.client.gui.screens.PresetFlatWorldScreen",
            "net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen",
            "net.minecraft.client.gui.screens.multiplayer.SafetyScreen",
            "net.minecraft.client.gui.screens.DirectJoinServerScreen",
            "net.minecraft.client.gui.screens.EditServerScreen",
            "net.minecraft.client.gui.screens.packs.PackSelectionScreen",
            "net.minecraft.client.gui.screens.telemetry.TelemetryInfoScreen",
            "net.minecraft.client.gui.screens.CreditsAndAttributionScreen",
            "net.minecraft.client.gui.screens.ConfirmScreen",
            "net.minecraft.client.gui.screens.ConfirmLinkScreen",
            "net.minecraft.client.gui.screens.BackupConfirmScreen",
            "net.neoforged.neoforge.client.gui.ModListScreen",
            "net.neoforged.neoforge.client.gui.ConfigurationScreen",
            "net.neoforged.neoforge.client.gui.ScrollableExperimentsScreen",
            "com.simibubi.create.infrastructure.gui.CreateMainMenuScreen");
    private static SoundInstance playing;
    private static long retryAt;
    private static Screen approvedScreen;

    private MenuMusic() {}

    /** Called after the screen-opening event has chosen its final destination. */
    public static void screenOpening(Screen current, Screen next) {
        if (current instanceof CwiTitleScreen) POLICY.screenOpening(true, false);
        POLICY.screenOpening(next instanceof CwiTitleScreen,
                next != null && MENU_CHILDREN.contains(next.getClass().getName()));
        approvedScreen = next;
    }

    public static void tick(Minecraft minecraft) {
        // Never keep a visit alive if a later event handler substituted a diagnostic screen.
        if (minecraft.screen instanceof CwiTitleScreen) {
            POLICY.screenOpening(true, false);
            approvedScreen = minecraft.screen;
        } else if (minecraft.screen != approvedScreen || minecraft.level != null) {
            POLICY.screenOpening(false, false);
            approvedScreen = minecraft.screen;
        }
        boolean artworkReady = BackgroundRenderer.isReady();
        if (POLICY.active() && minecraft.level == null && artworkReady) minecraft.getMusicManager().stopPlaying();
        boolean eligible = POLICY.mayPlay(minecraft.level != null, artworkReady, minecraft.getOverlay() != null,
                minecraft.options.getSoundSourceVolume(SoundSource.MASTER),
                minecraft.options.getSoundSourceVolume(SoundSource.MUSIC));
        if (!eligible) {
            stop(minecraft);
            retryAt = 0;
            return;
        }
        long now = Util.getMillis();
        if (playing != null && now >= retryAt && !minecraft.getSoundManager().isActive(playing)) {
            stop(minecraft);
        }
        if (playing == null && now >= retryAt) {
            playing = new Track();
            minecraft.getSoundManager().play(playing);
            retryAt = now + 1000;
        }
    }

    public static void onReload() {
        stop(Minecraft.getInstance());
        retryAt = 0;
    }

    private static void stop(Minecraft minecraft) {
        if (playing != null) {
            minecraft.getSoundManager().stop(playing);
            playing = null;
        }
    }

    private static final class Track extends AbstractSoundInstance {
        private Track() {
            super(CwiMenu.MENU_SOUND.get(), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            looping = true;
            delay = 0;
            volume = 1;
            pitch = 1;
            relative = true;
            attenuation = Attenuation.NONE;
        }
    }
}
