package net.quiwh.cwimenu.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import net.quiwh.cwimenu.CwiMenu;

public final class LastSession {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static SessionTarget target;
    private static boolean loaded;
    private static boolean usable;
    private static boolean opening;

    private LastSession() {}

    public static void refresh() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!loaded) {
            loaded = true;
            Path path = stateFile(minecraft);
            if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    if (Files.size(path) < 16_384) target = JSON.fromJson(Files.readString(path), SessionTarget.class);
                } catch (IOException | RuntimeException e) {
                    CwiMenu.LOGGER.warn("Ignoring unreadable CWI menu session state", e);
                }
            }
        }
        opening = false;
        usable = usable(minecraft, target);
    }

    public static boolean canResume() {
        if (!loaded) refresh();
        return usable && !opening;
    }

    public static Component description() {
        if (!canResume()) return Component.translatable("cwi_menu.no_session");
        String name = target.name();
        if (name == null || name.isBlank()) name = target.id();
        name = name.codePoints().filter(cp -> !Character.isISOControl(cp)).limit(256)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        return Component.translatable("local".equals(target.kind()) ? "cwi_menu.resume_local" : "cwi_menu.resume_server", name);
    }

    public static void resume(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (opening || minecraft.level != null || !usable(minecraft, target)) return;
        opening = true;
        if ("local".equals(target.kind())) {
            minecraft.createWorldOpenFlows().openWorld(target.id(), () -> minecraft.setScreen(parent));
        } else {
            ServerData server = new ServerData(target.name() == null ? target.id() : target.name(),
                    target.id(), ServerData.Type.OTHER);
            ConnectScreen.startConnecting(parent, minecraft, ServerAddress.parseString(target.id()), server, false, null);
        }
    }

    public static void rememberCurrent() {
        Minecraft minecraft = Minecraft.getInstance();
        SessionTarget remembered = null;
        if (minecraft.getSingleplayerServer() != null) {
            Path folder = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).normalize();
            remembered = new SessionTarget("local", folder.getFileName().toString(),
                    minecraft.getSingleplayerServer().getWorldData().getLevelName());
        } else if (minecraft.getCurrentServer() != null) {
            ServerData server = minecraft.getCurrentServer();
            remembered = new SessionTarget("server", server.ip, server.name);
        }
        if (remembered == null || !remembered.valid()) return;
        target = remembered;
        loaded = true;
        opening = false;
        usable = usable(minecraft, target);
        Path temporary = null;
        try {
            Path file = stateFile(minecraft);
            temporary = Files.createTempFile(file.getParent(), ".cwi-menu-session-", ".tmp");
            Files.writeString(temporary, JSON.toJson(target) + "\n");
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            CwiMenu.LOGGER.warn("Could not save CWI menu last session", e);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException e) { CwiMenu.LOGGER.debug("Could not clean temporary CWI session state", e); }
            }
        }
    }

    private static boolean usable(Minecraft minecraft, SessionTarget candidate) {
        if (candidate == null || !candidate.valid()) return false;
        if ("server".equals(candidate.kind())) return minecraft.allowsMultiplayer();
        Path world = minecraft.getLevelSource().getBaseDir().resolve(candidate.id());
        return Files.isDirectory(world, LinkOption.NOFOLLOW_LINKS)
                && (Files.isRegularFile(world.resolve("level.dat"), LinkOption.NOFOLLOW_LINKS)
                || Files.isRegularFile(world.resolve("level.dat_old"), LinkOption.NOFOLLOW_LINKS));
    }

    private static Path stateFile(Minecraft minecraft) {
        return minecraft.gameDirectory.toPath().resolve("cwi-menu-session.json");
    }
}
