package com.auradebug.modules;

import com.auradebug.AuraDebugAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

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

        rotationAngle += 2f;
        if (rotationAngle >= 360f) rotationAngle = 0f;

        scanTick++;
        if (scanTick >= 60 || spawners.isEmpty()) {
            scanTick = 0;
            scanSpawners();
        }

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
                        String mobType = getMobType(spawnerBE);
                        spawners.add(new SpawnerInfo(be.getPos().toImmutable(), mobType));
                    }
                }
            }
        }
    }

    private String getMobType(MobSpawnerBlockEntity spawner) {
        try {
            var nbt = spawner.createNbt(mc.world.getRegistryManager());
            if (nbt != null && nbt.contains("SpawnData")) {
                var spawnData = nbt.getCompound("SpawnData");
                if (spawnData.contains("entity")) {
                    var entity = spawnData.getCompound("entity");
                    if (entity.contains("id")) {
                        return entity.getString("id").replace("minecraft:", "").replace("_", " ");
                    }
                }
            }
        } catch (Exception ignored) {}
        return "Unbekannt";
    }

    private void renderSpawner(Render3DEvent event, SpawnerInfo info) {
        BlockPos pos = info.pos;
        double px = pos.getX() + 0.5;
        double py = pos.getY();
        double pz = pos.getZ() + 0.5;

        int height = lineHeight.get();
        Color color = lineColor.get();

        double hw = 0.5;
        double radAngle = Math.toRadians(rotationAngle);
        double cos = Math.cos(radAngle);
        double sin = Math.sin(radAngle);

        double x1 = px + hw * cos;
        double z1 = pz + hw * sin;
        double x2 = px - hw * cos;
        double z2 = pz - hw * sin;
        double topY = py + height;

        event.renderer.line(x1, py, z1, x1, topY, z1, color);
        event.renderer.line(x2, py, z2, x2, topY, z2, color);
        event.renderer.line(x1, topY, z1, x2, topY, z2, color);
        event.renderer.line(x1, py, z1, x2, py, z2, color);

        if (showLabel.get() && mc.player != null) {
            double dist = mc.player.getPos().distanceTo(new Vec3d(px, py + 0.5, pz));
            event.renderer.box(
                pos.getX(), (int) topY, pos.getZ(),
                pos.getX() + 1, (int) topY + 1, pos.getZ() + 1,
                color, color, ShapeMode.Lines, 0
            );
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        for (String w : s.split(" ")) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0)))
                .append(w.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }
}
