package net.islandcore.plugin.ranks;

import java.util.List;

public enum Rank {
    MEMBER("&7[&8Member&7]", "&f", List.of(
            "islandcore.visit",
            "islandcore.home",
            "islandcore.setspawn",
            "islandcore.myisland",
            "islandcore.msg",
            "islandcore.reply",
            "islandcore.rate",
            "islandcore.topislands",
            "islandcore.toggle",
            "islandcore.toggleislandvisits",
            "islandcore.skilltree"
    )),
    HELPER("&2[&a♢&2]", "&a", List.of(
            "islandcore.visit",
            "islandcore.home",
            "islandcore.setspawn",
            "islandcore.myisland",
            "islandcore.msg",
            "islandcore.reply",
            "islandcore.rate",
            "islandcore.topislands",
            "islandcore.toggle",
            "islandcore.toggleislandvisits",
            "islandcore.kick",
            "islandcore.invsee",
            "islandcore.enderchest",
            "islandcore.stafftp",
            "islandcore.skilltree"
    )),
    ADMIN("&6[&4♢&6]", "&4", List.of(
            "islandcore.visit",
            "islandcore.home",
            "islandcore.setspawn",
            "islandcore.myisland",
            "islandcore.msg",
            "islandcore.reply",
            "islandcore.rate",
            "islandcore.topislands",
            "islandcore.toggle",
            "islandcore.toggleislandvisits",
            "islandcore.kick",
            "islandcore.invsee",
            "islandcore.enderchest",
            "islandcore.stafftp",
            "islandcore.ban",
            "islandcore.unban",
            "islandcore.bypass",
            "islandcore.skilltree",
            "islandcore.tokens.spawn",
            "islandcore.resetratings"
    )),
    OWNER("&6[&d♢&6]", "&d", List.of(
            "islandcore.visit",
            "islandcore.home",
            "islandcore.setspawn",
            "islandcore.myisland",
            "islandcore.msg",
            "islandcore.reply",
            "islandcore.rate",
            "islandcore.topislands",
            "islandcore.toggle",
            "islandcore.toggleislandvisits",
            "islandcore.kick",
            "islandcore.invsee",
            "islandcore.enderchest",
            "islandcore.stafftp",
            "islandcore.ban",
            "islandcore.unban",
            "islandcore.bypass",
            "islandcore.rank.manage",
            "islandcore.skilltree",
            "islandcore.tokens.spawn",
            "islandcore.resetplayer",
            "islandcore.resetratings",
            "islandcore.ownerrate"
    ));

    public static final List<String> ALL_PERMISSIONS = List.of(
            "islandcore.visit", "islandcore.home", "islandcore.setspawn", "islandcore.myisland", "islandcore.msg",
            "islandcore.reply", "islandcore.rate", "islandcore.topislands", "islandcore.toggle", "islandcore.toggleislandvisits",
            "islandcore.invsee", "islandcore.enderchest", "islandcore.stafftp", "islandcore.kick",
            "islandcore.ban", "islandcore.unban", "islandcore.bypass",
            "islandcore.rank.manage", "islandcore.skilltree", "islandcore.tokens.spawn",
            "islandcore.resetplayer", "islandcore.resetratings", "islandcore.ownerrate"
    );

    private final String prefix;
    /** Colour code applied to the player's name in chat, per rank. */
    private final String nameColor;
    private final List<String> permissions;

    Rank(String prefix, String nameColor, List<String> permissions) {
        this.prefix = prefix;
        this.nameColor = nameColor;
        this.permissions = permissions;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getNameColor() {
        return nameColor;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public static Rank fromName(String name) {
        for (Rank rank : values()) {
            if (rank.name().equalsIgnoreCase(name)) {
                return rank;
            }
        }
        return null;
    }
}
