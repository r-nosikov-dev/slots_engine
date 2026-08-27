package com.slotengine.engine.catalog;

/**
 * Built-in slot skeletons. A template fills grid, symbols, paylines/ways, default
 * pays and reel weights so a new game is playable before any math tuning.
 */
public enum GameTemplateId {
    CLASSIC_10("classic-10", "5×3, 10 lines, fruit, no feature"),
    CLASSIC_20("classic-20", "5×3, 20 lines, wild + scatter free spins"),
    WAYS_243("ways-243", "5×3, 243 ways, sticky wilds in free spins"),
    CASCADE_20("cascade-20", "5×3, 20 lines, tumble + expanding wilds");

    private final String slug;
    private final String description;

    GameTemplateId(String slug, String description) {
        this.slug = slug;
        this.description = description;
    }

    public String slug() {
        return slug;
    }

    public String description() {
        return description;
    }

    public static GameTemplateId parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("template is required");
        }
        String key = raw.trim().toLowerCase().replace('_', '-');
        for (GameTemplateId id : values()) {
            if (id.slug.equals(key) || id.name().equalsIgnoreCase(raw.trim())) {
                return id;
            }
        }
        throw new IllegalArgumentException("Unknown template '" + raw + "'. Use classic-10, classic-20, ways-243, cascade-20");
    }
}
