package com.beegi.client.modules;

import com.beegi.client.BeegiClient;
import com.beegi.client.core.ModuleCatalog;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Beegi 26.3 ESP and Fly additions.
 * ESP is an honest HUD projection of loaded client-side entities and blocks; it
 * does not reveal unloaded or server-hidden data. All scanners are bounded.
 */
public final class BeegiAddons {
    private static final int MAX_ORES = 80;
    private static final int MAX_STORAGES = 80;
    private static final int SCAN_RADIUS = 12;
    private static final List<BlockPos> ORES = new ArrayList<>();
    private static final List<BlockPos> STORAGES = new ArrayList<>();
    private static Object lastLevel;
    private static int ticks;

    private BeegiAddons() {}

    public static void registerHud() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("beegi", "esp_overlay"), BeegiAddons::drawHud);
    }

    public static void tick(Minecraft mc, ModuleCatalog catalog) {
        if (mc.level != lastLevel) {
            lastLevel = mc.level;
            ORES.clear();
            STORAGES.clear();
            ticks = 0;
        }
        if (mc.player == null || mc.level == null) return;
        if (mc.gui.screen() == null && catalog.enabled("fly")) fly(mc);
        ticks++;
        if ((!catalog.enabled("block-esp") && !catalog.enabled("storage-esp"))) {
            ORES.clear();
            STORAGES.clear();
            return;
        }
        if (ticks % 20 == 1) scan(mc, catalog);
    }

    /** Normal collision and server movement validation still apply. */
    private static void fly(Minecraft mc) {
        double forward = (mc.options.keyUp.isDown() ? 1 : 0)
                - (mc.options.keyDown.isDown() ? 1 : 0);
        double strafe = (mc.options.keyRight.isDown() ? 1 : 0)
                - (mc.options.keyLeft.isDown() ? 1 : 0);
        double scale = Math.hypot(forward, strafe);
        if (scale > 0) { forward /= scale; strafe /= scale; }
        double yaw = Math.toRadians(mc.player.getYRot());
        double vx = (-Math.sin(yaw) * forward - Math.cos(yaw) * strafe) * .38;
        double vz = ( Math.cos(yaw) * forward - Math.sin(yaw) * strafe) * .38;
        double vy = mc.options.keyJump.isDown() ? .32 :
                mc.options.keyShift.isDown() ? -.32 : 0.0;
        mc.player.setDeltaMovement(vx, vy, vz);
        mc.player.fallDistance = 0.0f; // local visual state; server may still apply fall damage
    }

    private static void scan(Minecraft mc, ModuleCatalog catalog) {
        ORES.clear();
        STORAGES.clear();
        BlockPos origin = mc.player.blockPosition();
        boolean ores = catalog.enabled("block-esp");
        boolean storage = catalog.enabled("storage-esp");
        for (int y = -7; y <= 7; y++)
            for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++)
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    if ((!ores || ORES.size() >= MAX_ORES)
                            && (!storage || STORAGES.size() >= MAX_STORAGES)) return;
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    if (ores && ORES.size() < MAX_ORES && isOre(state)) ORES.add(pos.immutable());
                    if (storage && STORAGES.size() < MAX_STORAGES && isStorage(state)) STORAGES.add(pos.immutable());
                }
    }

    private static boolean isOre(BlockState state) {
        return state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                || state.is(Blocks.ANCIENT_DEBRIS)
                || state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)
                || state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE)
                || state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)
                || state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE);
    }

    private static boolean isStorage(BlockState state) {
        Block b = state.getBlock();
        return b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.BARREL
                || b == Blocks.ENDER_CHEST || b == Blocks.SHULKER_BOX
                || b == Blocks.WHITE_SHULKER_BOX || b == Blocks.BLACK_SHULKER_BOX
                || b == Blocks.RED_SHULKER_BOX || b == Blocks.BLUE_SHULKER_BOX
                || b == Blocks.GREEN_SHULKER_BOX || b == Blocks.PURPLE_SHULKER_BOX;
    }

    public static void drawHud(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.gui.screen() != null) return;
        ModuleCatalog catalog = BeegiClient.CATALOG;
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        Vec3 eye = mc.player.getEyePosition();
        if (catalog.enabled("esp")) {
            for (Player player : mc.level.players()) {
                if (player == mc.player || !player.isAlive() || player.isSpectator()) continue;
                if (eye.distanceToSqr(player.position()) > 64 * 64) continue;
                drawProjected(g, mc, eye, width, height, player.getBoundingBox(), 0xFF4DDEFF,
                        player.getName().getString());
            }
        }
        if (catalog.enabled("block-esp")) {
            for (BlockPos pos : ORES) drawProjected(g, mc, eye, width, height,
                    new AABB(pos), 0xFF50E4B1, "Ore");
        }
        if (catalog.enabled("storage-esp")) {
            for (BlockPos pos : STORAGES) drawProjected(g, mc, eye, width, height,
                    new AABB(pos), 0xFFFFB64A, "Storage");
        }
    }

    private static void drawProjected(GuiGraphicsExtractor g, Minecraft mc, Vec3 eye,
                                      int width, int height, AABB box, int color, String label) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (int ix = 0; ix < 2; ix++)
            for (int iy = 0; iy < 2; iy++)
                for (int iz = 0; iz < 2; iz++) {
                    double x = ix == 0 ? box.minX : box.maxX;
                    double y = iy == 0 ? box.minY : box.maxY;
                    double z = iz == 0 ? box.minZ : box.maxZ;
                    double[] p = project(mc, eye, x, y, z, width, height);
                    if (p == null) return; // avoid huge projections crossing camera plane
                    minX = Math.min(minX, p[0]); minY = Math.min(minY, p[1]);
                    maxX = Math.max(maxX, p[0]); maxY = Math.max(maxY, p[1]);
                }
        int l = (int)Math.max(0, Math.min(width-1, minX));
        int t = (int)Math.max(0, Math.min(height-1, minY));
        int r = (int)Math.max(0, Math.min(width, maxX));
        int b = (int)Math.max(0, Math.min(height, maxY));
        if (r - l < 3 || b - t < 3 || r <= 0 || b <= 0 || l >= width || t >= height) return;
        g.fill(l, t, r, t+1, color);
        g.fill(l, b-1, r, b, color);
        g.fill(l, t, l+1, b, color);
        g.fill(r-1, t, r, b, color);
        g.text(mc.font, label, l, Math.max(1, t-10), color, true);
    }

    /** FOV-based, first-person screen projection; no access to hidden server data. */
    private static double[] project(Minecraft mc, Vec3 eye, double x, double y, double z,
                                    int width, int height) {
        double yaw = Math.toRadians(mc.player.getYRot());
        double pitch = Math.toRadians(mc.player.getXRot());
        double sy = Math.sin(yaw), cy = Math.cos(yaw);
        double sp = Math.sin(pitch), cp = Math.cos(pitch);
        double dx = x - eye.x, dy = y - eye.y, dz = z - eye.z;
        double front = dx * (-sy * cp) + dy * (-sp) + dz * (cy * cp);
        if (front <= .20) return null;
        double right = dx * (-cy) + dz * (-sy);
        double up = dx * (-sy * sp) + dy * cp + dz * (cy * sp);
        double fov = Math.toRadians(Math.max(30, Math.min(110, mc.options.fov().get())));
        double focal = height / (2 * Math.tan(fov / 2));
        return new double[]{width / 2.0 + right / front * focal,
                height / 2.0 - up / front * focal};
    }
}
