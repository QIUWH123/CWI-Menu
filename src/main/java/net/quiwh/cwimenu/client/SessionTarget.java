package net.quiwh.cwimenu.client;

public record SessionTarget(String kind, String id, String name) {
    public boolean valid() {
        if ("local".equals(kind)) return validLocalId(id);
        return "server".equals(kind) && id != null && !id.isBlank() && id.length() <= 4096
                && id.chars().noneMatch(Character::isISOControl);
    }

    public static boolean validLocalId(String id) {
        return id != null && !id.isBlank() && id.length() <= 255
                && !id.equals(".") && !id.equals("..")
                && id.indexOf('/') < 0 && id.indexOf('\\') < 0 && id.indexOf(':') < 0
                && id.chars().noneMatch(Character::isISOControl);
    }
}
