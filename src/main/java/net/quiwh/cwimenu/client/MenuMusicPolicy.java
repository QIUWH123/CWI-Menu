package net.quiwh.cwimenu.client;

/** A soundtrack visit starts at CWI's title and continues only through known menu screens. */
public final class MenuMusicPolicy {
    private boolean menuVisit;

    public void screenOpening(boolean customTitle, boolean menuChild) {
        menuVisit = customTitle || (menuVisit && menuChild);
    }

    public boolean active() {
        return menuVisit;
    }

    public boolean mayPlay(boolean hasLevel, boolean artworkReady, boolean overlay,
                           float masterVolume, float musicVolume) {
        return menuVisit && !hasLevel && artworkReady && !overlay && masterVolume > 0 && musicVolume > 0;
    }
}
