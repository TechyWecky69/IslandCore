package com.islandcore.anticheat.check;

public enum CheckType {
    SPEED("Speed", "checks.speed"),
    FLY("Fly", "checks.fly"),
    NOFALL("NoFall", "checks.nofall"),
    JESUS("Jesus", "checks.jesus"),
    ROTATION("Rotation", "checks.rotation"),
    REACH("Reach", "checks.reach"),
    KILLAURA("KillAura", "checks.killaura"),
    AUTOCLICKER("AutoClicker", "checks.autoclicker"),
    FASTBREAK("FastBreak", "checks.fastbreak"),
    DUPECLICK("DupeClick", "checks.dupeclick"),
    NESTEDCONTAINER("NestedContainer", "checks.nestedcontainer");

    private final String displayName;
    private final String configPath;

    CheckType(String displayName, String configPath) {
        this.displayName = displayName;
        this.configPath = configPath;
    }

    public String getDisplayName() { return displayName; }
    public String getConfigPath() { return configPath; }

    public static CheckType fromString(String name) {
        for (CheckType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.displayName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
