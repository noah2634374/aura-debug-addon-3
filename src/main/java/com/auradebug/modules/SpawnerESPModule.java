package com.auradebug.modules;

import com.auradebug.AuraDebugAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class SpawnerESPModule extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Farbe der rotierenden Linie über dem Spawner.")
        .defaultValue(new SettingColor(255, 0, 0, 220))
        .build()
    );

    private final Setting<Integer> lineHeight = sgGeneral.add(new IntSetting.Builder()
        .name("line-height")
        .description("Höhe der Linie über dem Spawner in Blöcken.")
        .defaultValue(32)
        .min(8)
        .sliderMax(128)
        .build()
    );

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("search-radius")
        .description("Suchradius für Spawner in Chunks.")
        .defaultValue(6)
        .min(1)
        .sliderMax(16)
        .build()
    );

    private final Setting<Boolean> showLabel = sgGeneral.add(new BoolSetting.Builder()
        .name("show-label")
        .description("Zeigt Mob-Typ und Entfernung über dem Spawner an.")
        .defaultValue(true)
        .build()
    );

    private float rotationAngle = 0f;
    private final List<SpawnerInfo> spawners = new ArrayList<>();
    private int scanTick = 0;

    public static class SpawnerInfo {
        public BlockPos pos;
        public String mobType;

        public SpawnerInfo(BlockPos pos, String mobType) {
            this.pos = pos;
            this.mobType = mobType;
        }
    }

    public SpawnerESPModule() {
        super(AuraDebugAddon.CATEGORY, "spawner-esp",
            "Zeigt Spawner mit rotierender roter Linie und Mob-Typ-Beschriftung an.");
    }

    @Override
    public void onActivate() {
        spawners.clear();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        // Rotate line
        rotationAngle += 2f;
        if (rotationAngle >= 360f) rotationAngle = 0f;

        // Rescan every 60 ticks
        scanTick++;
        if (scanTick >= 60 || spawners.isEmpty()) {
            scanTick = 0;
            scanSpawners();
        }

        // Render each spawner
        for (SpawnerInfo info : spawners) {
            renderSpawner(event, info);
        }
    }

    private void scanSpawners() {
        if (mc.world == null || mc.player == null) return;
        spawners.clear();

        int cx = mc.player.getChunkPos().x;
        int cz = mc.player.getChunkPos().z;
        int r = radius.get();

        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                var chunk = mc.world.getChunk(x, z);
                if (chunk == null) continue;

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof MobSpawnerBlockEntity spawnerBE) {
                        String mobType = getMobType(mc.world, spawnerBE);
                        spawners.add(new SpawnerInfo(be.getPos().toImmutable(), mobType));
                    }
                }
            }
        }
    }

    private String getMobType(World world, MobSpawnerBlockEntity spawner) {
        try {
            var logic = spawner.getLogic();
            // Access spawn entry via reflection or nbt
            var nbt = spawner.createNbt(world.getRegistryManager());
            if (nbt != null && nbt.contains("SpawnData")) {
                var spawnData = nbt.getCompound("SpawnData");
                if (spawnData.contains("entity")) {
                    var entity = spawnData.getCompound("entity");
                    if (entity.contains("id")) {
                        String id = entity.getString("id");
                        // Strip "minecraft:" prefix
                        return id.replace("minecraft:", "").replace("_", " ");
                    }
                }
            }
        } catch (Exception e) {
            // fallback
        }
        return "Unbekannt";
    }

    private void renderSpawner(Render3DEvent event, SpawnerInfo info) {
        BlockPos pos = info.pos;
        double px = pos.getX() + 0.5;
        double py = pos.getY();
        double pz = pos.getZ() + 0.5;

        int height = lineHeight.get();
        Color color = lineColor.get();

        // The spawner is 1x1 block wide. Draw a rotating rectangle/line
        // Width matches spawner (1 block wide = 0.5 half-width from center)
        double hw = 0.5; // half width of spawner

        double radAngle = Math.toRadians(rotationAngle);
        double cos = Math.cos(radAngle);
        double sin = Math.sin(radAngle);

        // Two corners of the top of the rotating "plank"
        double x1 = px + hw * cos;
        double z1 = pz + hw * sin;
        double x2 = px - hw * cos;
        double z2 = pz - hw * sin;

        double topY = py + height;

        // Draw 4 lines forming a rotating plane (bottom-left to top-left, bottom-right to top-right, connecting tops)
        event.renderer.line(x1, py, z1, x1, topY, z1, color);
        event.renderer.line(x2, py, z2, x2, topY, z2, color);
        event.renderer.line(x1, topY, z1, x2, topY, z2, color);
        event.renderer.line(x1, py, z1, x2, py, z2, color);

        // Label above spawner
        if (showLabel.get() && mc.player != null) {
            double dist = mc.player.getPos().distanceTo(new Vec3d(px, py + 0.5, pz));
            String label = capitalize(info.mobType) + " §7(" + (int) dist + "m)";

            // Render name tag above spawner
            RenderUtils.renderText(event.matrices,
                net.minecraft.text.Text.of("§c" + label),
                pos.getX() + 0.5f,
                pos.getY() + 1.5f,
                pos.getZ() + 0.5f,
                1.0f,
                true
            );
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                sb.append(w.substring(1).toLowerCase());
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }
}
