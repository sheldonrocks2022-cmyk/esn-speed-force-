package com.esnstudios.speedforce;

import org.bukkit.Color;
import org.bukkit.Material;
import java.util.Locale;

public enum Suit {
    FLASH("Flash", "flash", Color.fromRGB(210, 30, 35), 0.44f, 1.0, Material.RED_DYE),
    REVERSE("Reverse Flash", "reverse", Color.fromRGB(245, 206, 38), 0.48f, 1.5, Material.YELLOW_DYE),
    ZOOM("Zoom", "zoom", Color.fromRGB(26, 26, 43), 0.52f, 1.2, Material.BLACK_DYE),
    GODSPEED("Godspeed", "godspeed", Color.fromRGB(245, 245, 230), 0.55f, 1.5, Material.WHITE_DYE),
    SAVITAR("Savitar", "savitar", Color.fromRGB(75, 96, 180), 0.43f, 3.0, Material.BLUE_DYE),
    ESN("ESN Phantom", "esn", Color.fromRGB(135, 38, 218), 0.56f, 2.0, Material.PURPLE_DYE);
    public final String name, id;
    public final Color color;
    public final float walkSpeed;
    public final double hitBonus;
    public final Material dye;
    Suit(String name, String id, Color color, float walkSpeed, double hitBonus, Material dye) {
        this.name=name; this.id=id; this.color=color; this.walkSpeed=walkSpeed; this.hitBonus=hitBonus; this.dye=dye;
    }
    public static Suit of(String raw) {
        if (raw == null) return null;
        String q=raw.toLowerCase(Locale.ROOT).replace("_","").replace("-","").replace(" ","");
        for (Suit s: values()) if (s.id.equals(q) || s.name.toLowerCase(Locale.ROOT).replace(" ","").equals(q)) return s;
        return null;
    }
}
