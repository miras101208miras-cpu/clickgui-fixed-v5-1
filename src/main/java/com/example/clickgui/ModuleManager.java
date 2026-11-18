package com.example.clickgui;

import java.util.*;

public class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();

    static {
        // ---- работающие визуальные эффекты ----
        MODULES.add(new Module("Target ESP", Category.VISUALS,
                new Setting.Multi("Style", List.of("Ring", "Orbit", "Souls", "Pulse", "Glow", "Skeleton",
                        "Halo", "Lock-On", "Circle", "Health", "Frost"), "Souls"),
                new Setting.Slider("Speed", 1.0, 0.2, 3.0, 0.1),
                new Setting.Slider("Size", 1.0, 0.5, 2.0, 0.05)));
        MODULES.add(new Module("China Hat", Category.VISUALS,
                new Setting.Slider("Radius", 0.6, 0.3, 1.0, 0.05),
                new Setting.Slider("Height", 0.3, 0.1, 0.8, 0.05)));
        MODULES.add(new Module("Jump Circles", Category.VISUALS,
                new Setting.Slider("Radius", 1.2, 0.5, 3.0, 0.1),
                new Setting.Slider("Time", 1.0, 0.3, 3.0, 0.1)));
        MODULES.add(new Module("Trails", Category.VISUALS,
                new Setting.Slider("Length", 1.0, 0.2, 3.0, 0.1),
                new Setting.Slider("Height", 0.6, 0.1, 1.8, 0.1)));
        MODULES.add(new Module("Hit Particles", Category.VISUALS,
                new Setting.Multi("Type", List.of("Snow", "Hearts", "Stars"), "Hearts"),
                new Setting.Slider("Count", 14, 4, 40, 1)));
        MODULES.add(new Module("World Particles", Category.VISUALS,
                new Setting.Multi("Type", List.of("Snow", "Hearts", "Stars"), "Snow"),
                new Setting.Slider("Count", 60, 10, 200, 5),
                new Setting.Slider("Radius", 12, 4, 32, 1)));
        MODULES.add(new Module("Hit Color", Category.VISUALS,
                new Setting.Slider("Red", 170, 0, 255, 1),
                new Setting.Slider("Green", 80, 0, 255, 1),
                new Setting.Slider("Blue", 255, 0, 255, 1),
                new Setting.Slider("Alpha", 140, 0, 255, 1)));
        MODULES.add(new Module("Kill Effect", Category.VISUALS,
                new Setting.Multi("Effect", List.of("Lightning", "Particles"), "Lightning")));
        MODULES.add(new Module("Self Nametag", Category.VISUALS));
        MODULES.add(new Module("Arrow Nametag", Category.VISUALS,
                new Setting.Bool("Type", true), new Setting.Bool("Distance", true)));
        MODULES.add(new Module("Render Tweaks", Category.VISUALS,
                new Setting.Bool("No Hurt Cam", true), new Setting.Bool("No Fire Overlay", true)));
        MODULES.add(new Module("Full Bright", Category.WORLD));
        // ---- HUD (рисуется в Hud.java, двигается в HudEditorScreen) ----
        MODULES.add(new Module("Watermark", Category.HUD, new Setting.Bool("Time", true)));
        MODULES.add(new Module("Coordinates", Category.HUD));
        MODULES.add(new Module("FPS Counter", Category.HUD));
        MODULES.add(new Module("Armor HUD", Category.HUD));
        MODULES.add(new Module("Keystrokes", Category.HUD, new Setting.Bool("CPS", true)));
        // ---- заглушки (пока без логики) ----
        MODULES.add(new Module("Zoom", Category.CAMERA, new Setting.Slider("Zoom", 3.0, 1.5, 10.0, 0.5)));
        MODULES.add(new Module("FOV Changer", Category.CAMERA, new Setting.Slider("FOV", 90, 30, 120, 1)));
        MODULES.add(new Module("Time Changer", Category.WORLD, new Setting.Slider("Time", 6.0, 0.0, 24.0, 0.5)));
        MODULES.add(new Module("Weather Changer", Category.WORLD,
                new Setting.Mode("Weather", List.of("Clear", "Rain", "Thunder"), 0)));
        MODULES.add(new Module("Custom Sky", Category.WORLD,
                new Setting.Mode("Sky", List.of("Aurora Night", "Galaxy", "Sakura Sunset", "Blood Moon", "Neon Dusk", "Cotton Candy",
                        "Black Hole", "Ice Dawn", "Thunderstorm", "Golden Hour", "Moonlit Night", "Pink Nebula",
                        "Violet Cosmos", "Pastel Planets", "Anime Clouds"), 0),
                new Setting.Slider("Spin", 0.0, 0.0, 1.0, 0.05)));
        MODULES.add(new Module("World Customizer", Category.WORLD,
                new Setting.Slider("Sky R", 120, 0, 255, 1), new Setting.Slider("Sky G", 80, 0, 255, 1), new Setting.Slider("Sky B", 220, 0, 255, 1),
                new Setting.Slider("Fog R", 255, 0, 255, 1), new Setting.Slider("Fog G", 150, 0, 255, 1), new Setting.Slider("Fog B", 200, 0, 255, 1)));
        // ---- цвет всех эффектов ----
        MODULES.add(new Module("Theme", Category.MISC,
                new Setting.Mode("Theme", List.of(Theme.NAMES), 0),
                new Setting.Slider("Hue", 0.85, 0.0, 1.0, 0.01),
                new Setting.Bool("Rainbow", false)));
    }

    public static List<Module> byCategory(Category c) {
        return MODULES.stream().filter(m -> m.category == c).toList();
    }

    public static Module get(String name) {
        for (Module m : MODULES) if (m.name.equals(name)) return m;
        throw new IllegalArgumentException(name);
    }
}
