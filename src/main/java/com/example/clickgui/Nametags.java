package com.example.clickgui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Locale;

/** Self Nametag и Arrow Nametag: метки рисуются на HUD поверх мира. */
public final class Nametags {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void render(DrawContext ctx, float td) {
        if (mc.player == null || mc.world == null || !Effects.camReady) return;

        Module self = ModuleManager.get("Self Nametag");
        if (self.enabled && !mc.options.getPerspective().isFirstPerson()) {
            Vec3d p = mc.player.getLerpedPos(td).add(0, mc.player.getHeight() + 0.5, 0);
            label(ctx, p, mc.player.getName().getString(), Theme.accent());
        }

        Module ar = ModuleManager.get("Arrow Nametag");
        if (!ar.enabled) return;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof PersistentProjectileEntity)) continue;
            if (e.getVelocity().lengthSquared() < 0.01) continue;      // только летящие
            double dist = mc.player.distanceTo(e);
            if (dist > 48) continue;
            Vec3d p = e.getLerpedPos(td).add(0, 0.35, 0);
            if (!visible(p)) continue;                                 // сквозь стены не показываем
            String type = e instanceof TridentEntity ? "Trident" : e instanceof SpectralArrowEntity ? "Spectral" : "Arrow";
            StringBuilder sb = new StringBuilder();
            if (ar.bool("Type")) sb.append(type);
            if (ar.bool("Distance")) {
                if (sb.length() > 0) sb.append("  ");
                sb.append(String.format(Locale.ROOT, "%.1fm", dist));
            }
            if (sb.length() > 0) label(ctx, p, sb.toString(), 0xFFFFFFFF);
        }
    }

    private static boolean visible(Vec3d p) {
        return mc.world.raycast(new RaycastContext(Effects.camPos(), p, RaycastContext.ShapeType.VISUAL,
                RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;
    }

    private static void label(DrawContext ctx, Vec3d world, String s, int col) {
        float[] sp = project(world, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight());
        if (sp == null) return;
        double dist = world.distanceTo(Effects.camPos());
        float scale = (float) Math.max(0.5, Math.min(1.0, 6.0 / Math.max(dist, 0.1)));
        int w = Hud.tw(s);
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(sp[0], sp[1]);
        m.scale(scale, scale);
        ClickGuiScreen.roundRect(ctx, -w / 2 - 4, -6, w + 8, 12, 3, 0x99101620);
        Hud.txt(ctx, s, -w / 2, -4, col);
        m.popMatrix();
    }

    /** Мировая точка -> точка на экране (null, если за спиной). */
    static float[] project(Vec3d p, int sw, int sh) {
        Vec3d cam = Effects.camPos();
        Vector3f rel = new Vector3f((float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z));
        new Quaternionf(Effects.camRot).conjugate().transform(rel);
        if (rel.z > -0.05f) return null;
        double f = 1.0 / Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2);
        double aspect = (double) sw / sh;
        double nx = rel.x * f / aspect / -rel.z, ny = rel.y * f / -rel.z;
        if (Math.abs(nx) > 1.2 || Math.abs(ny) > 1.2) return null;
        return new float[]{(float) ((nx + 1) / 2 * sw), (float) ((1 - ny) / 2 * sh)};
    }
}
