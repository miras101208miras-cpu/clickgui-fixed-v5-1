package com.example.clickgui;

import com.google.gson.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.*;
import java.time.LocalTime;
import java.util.*;

/** HUD: rendering of HUD-tab modules + movable positions (config/clickgui/hud.json). */
public final class Hud {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final StyleSpriteSource FONT = new StyleSpriteSource.Font(Identifier.of("clickgui", "main"));
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("clickgui/hud.json");
    private static final int BG = 0x99101620, WHITE = 0xFFFFFFFF, GRAY = 0xFFB4BECB, DARK = 0xFF1A1D24;

    /** Elements that can be moved. Name = module name in ModuleManager. */
    public static final String[] ELEMENTS = {"Watermark", "Coordinates", "FPS Counter", "Armor HUD", "Keystrokes"};
    private static final float[][] DEFAULTS = {{0.01f, 0.01f}, {0.01f, 0.08f}, {0.01f, 0.14f}, {0.5f, 0.86f}, {0.01f, 0.75f}};

    private static final Map<String, float[]> POS = new LinkedHashMap<>();   // relX, relY (0..1)
    private static final Map<String, int[]> SIZE = new HashMap<>();         // w, h of last frame
    private static final ArrayDeque<Long> lmb = new ArrayDeque<>(), rmb = new ArrayDeque<>();
    private static boolean lmbWas, rmbWas;

    public static void init() {
        for (int i = 0; i < ELEMENTS.length; i++) POS.put(ELEMENTS[i], DEFAULTS[i].clone());
        load();
        HudElementRegistry.addLast(Identifier.of("clickgui", "hud"), (ctx, tick) -> {
            if (mc.player == null || mc.options.hudHidden) return;
            Nametags.render(ctx, tick.getTickProgress(true));
            if (mc.currentScreen instanceof HudEditorScreen) return; // editor draws HUD itself
            renderAll(ctx);
        });
    }

    // ---------------- positions ----------------
    public static int w(String n) { int[] s = SIZE.get(n); return s == null ? 40 : s[0]; }
    public static int h(String n) { int[] s = SIZE.get(n); return s == null ? 12 : s[1]; }
    public static int x(String n) { return Math.round(POS.get(n)[0] * Math.max(0, mc.getWindow().getScaledWidth() - w(n))); }
    public static int y(String n) { return Math.round(POS.get(n)[1] * Math.max(0, mc.getWindow().getScaledHeight() - h(n))); }

    public static void setPos(String n, int x, int y) {
        int mw = Math.max(1, mc.getWindow().getScaledWidth() - w(n)), mh = Math.max(1, mc.getWindow().getScaledHeight() - h(n));
        POS.put(n, new float[]{clamp((float) x / mw), clamp((float) y / mh)});
    }

    public static void reset(String n) {
        for (int i = 0; i < ELEMENTS.length; i++) if (ELEMENTS[i].equals(n)) POS.put(n, DEFAULTS[i].clone());
    }

    public static boolean visible(String n) { return ModuleManager.get(n).enabled; }

    // ---------------- rendering ----------------
    public static void renderAll(DrawContext ctx) {
        for (String n : ELEMENTS) {
            if (!visible(n)) continue;
            int[] wh = draw(ctx, n, x(n), y(n));
            SIZE.put(n, wh);
        }
    }

    private static int[] draw(DrawContext c, String n, int x, int y) {
        return switch (n) {
            case "Watermark" -> watermark(c, x, y);
            case "Coordinates" -> {
                var p = mc.player.getBlockPos();
                yield label(c, x, y, "XYZ", p.getX() + " " + p.getY() + " " + p.getZ());
            }
            case "FPS Counter" -> label(c, x, y, "FPS", String.valueOf(mc.getCurrentFps()));
            case "Armor HUD" -> armor(c, x, y);
            case "Keystrokes" -> keystrokes(c, x, y);
            default -> new int[]{0, 0};
        };
    }

    private static int[] watermark(DrawContext c, int x, int y) {
        Module m = ModuleManager.get("Watermark");
        String s = "ClickGUI  |  " + mc.getCurrentFps() + " fps";
        if (m.bool("Time")) { LocalTime t = LocalTime.now(); s += String.format("  |  %02d:%02d", t.getHour(), t.getMinute()); }
        int w = tw(s) + 12, h = 14;
        ClickGuiScreen.roundRect(c, x, y, w, h, 4, BG);
        c.fill(x + 3, y, x + w - 3, y + 1, Theme.accent());
        txt(c, s, x + 6, y + 3, WHITE);
        return new int[]{w, h};
    }

    private static int[] label(DrawContext c, int x, int y, String key, String val) {
        int w = tw(key) + tw(val) + 16, h = 14;
        ClickGuiScreen.roundRect(c, x, y, w, h, 4, BG);
        txt(c, key, x + 6, y + 3, Theme.accent());
        txt(c, val, x + 10 + tw(key), y + 3, WHITE);
        return new int[]{w, h};
    }

    private static int[] armor(DrawContext c, int x, int y) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
        int w = slots.length * 18 + 4, h = 22;
        ClickGuiScreen.roundRect(c, x, y, w, h, 4, BG);
        for (int i = 0; i < slots.length; i++) {
            ItemStack st = mc.player.getEquippedStack(slots[i]);
            if (st.isEmpty()) continue;
            int ix = x + 3 + i * 18, iy = y + 3;
            c.drawItem(st, ix, iy);
            c.drawStackOverlay(mc.textRenderer, st, ix, iy);
        }
        return new int[]{w, h};
    }

    private static int[] keystrokes(DrawContext c, int x, int y) {
        var o = mc.options;
        long now = System.currentTimeMillis();
        boolean l = o.attackKey.isPressed(), r = o.useKey.isPressed();
        if (l && !lmbWas) lmb.add(now);
        if (r && !rmbWas) rmb.add(now);
        lmbWas = l; rmbWas = r;
        while (!lmb.isEmpty() && now - lmb.peekFirst() > 1000) lmb.pollFirst();
        while (!rmb.isEmpty() && now - rmb.peekFirst() > 1000) rmb.pollFirst();
        boolean cps = ModuleManager.get("Keystrokes").bool("CPS");

        int k = 18, g = 2, w = k * 3 + g * 2;
        key(c, x + k + g, y, k, k, "W", o.forwardKey.isPressed());
        key(c, x, y + k + g, k, k, "A", o.leftKey.isPressed());
        key(c, x + k + g, y + k + g, k, k, "S", o.backKey.isPressed());
        key(c, x + (k + g) * 2, y + k + g, k, k, "D", o.rightKey.isPressed());
        int my = y + (k + g) * 2, half = (w - g) / 2;
        key(c, x, my, half, k, cps ? lmb.size() + " CPS" : "LMB", l);
        key(c, x + half + g, my, half, k, cps ? rmb.size() + " CPS" : "RMB", r);
        key(c, x, my + k + g, w, 10, "___", o.jumpKey.isPressed());
        return new int[]{w, (k + g) * 3 + 10};
    }

    private static void key(DrawContext c, int x, int y, int w, int h, String s, boolean down) {
        ClickGuiScreen.roundRect(c, x, y, w, h, 4, down ? Theme.accent() : BG);
        txt(c, s, x + (w - tw(s)) / 2, y + (h - 8) / 2, down ? DARK : WHITE);
    }

    // ---------------- save / load ----------------
    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (var e : POS.entrySet()) {
                JsonObject o = new JsonObject();
                o.addProperty("x", e.getValue()[0]);
                o.addProperty("y", e.getValue()[1]);
                root.add(e.getKey(), o);
            }
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private static void load() {
        if (!Files.exists(FILE)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            for (String n : ELEMENTS) if (root.has(n)) {
                JsonObject o = root.getAsJsonObject(n);
                POS.put(n, new float[]{clamp(o.get("x").getAsFloat()), clamp(o.get("y").getAsFloat())});
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ---------------- utils ----------------
    static Text t(String s) { return Text.literal(s).setStyle(Style.EMPTY.withFont(FONT)); }
    static int tw(String s) { return mc.textRenderer.getWidth(t(s)); }
    static void txt(DrawContext c, String s, int x, int y, int col) { c.drawText(mc.textRenderer, t(s), x, y, col, false); }
    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
