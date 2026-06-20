package com.auradebug.modules;

import com.auradebug.AuraDebugAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.*;

public class ChunkFinderModule extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> chunkRadius = sgGeneral.add(new IntSetting.Builder()
        .name("chunk-radius")
        .description("Wie viele Chunks um den Spieler herum gescannt werden.")
        .defaultValue(5)
        .min(1)
        .sliderMax(12)
        .build()
    );

    private final Setting<Integer> minStorageCount = sgGeneral.add(new IntSetting.Builder()
        .name("min-storage-count")
        .description("Minimale Anzahl an Storage-Blöcken (Kisten etc.) pro Chunk um ihn zu markieren.")
        .defaultValue(5)
        .min(1)
        .sliderMax(50)
        .build()
    );

    private final Setting<SettingColor> spawnerChunkColor = sgGeneral.add(new ColorSetting.Builder()
        .name("spawner-chunk-color")
        .description("Farbe für Chunks mit Spawnern unter Deepslate.")
        .defaultValue(new SettingColor(255, 0, 0, 80))
        .build()
    );

    private final Setting<SettingColor> storageChunkColor = sgGeneral.add(new ColorSetting.Builder()
        .name("storage-chunk-color")
        .description("Farbe für Chunks mit Storage-Blöcken unter Deepslate.")
        .defaultValue(new SettingColor(255, 105, 180, 80))
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("Wie die Chunk-Hervorhebungen gerendert werden.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private static class ChunkResult {
        ChunkPos pos;
        boolean hasSpawner;
        boolean hasStorage;
        int storageCount;

        ChunkResult(ChunkPos pos) {
            this.pos = pos;
        }
    }

    private final Map<Long, ChunkResult> results = new HashMap<>();
    private int scanTick = 0;

    public ChunkFinderModule() {
        super(AuraDebugAddon.CATEGORY, "chunk-finder",
            "Scannt Chunks nach Spawnern (rot) und Storage-Blöcken (pink) unter dem Deepslate-Layer. Min. " +
            "Storage-Anzahl konfigurierbar.");
    }

    @Override
    public void onActivate() {
        results.clear();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        scanTick++;
        if (scanTick >= 100) {
            scanTick = 0;
            scan();
        }

        renderResults(event);
    }

    private void scan() {
        if (mc.world == null || mc.player == null) return;
        results.clear();

        int cx = mc.player.getChunkPos().x;
        int cz = mc.player.getChunkPos().z;
        int r = chunkRadius.get();

        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                var chunk = mc.world.getChunk(x, z);
                if (chunk == null) continue;

                ChunkPos cpos = new ChunkPos(x, z);
                ChunkResult result = new ChunkResult(cpos);

                // Scan only below Y=0 (deepslate zone)
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos bpos = be.getPos();
                    if (bpos.getY() > 0) continue; // Only deepslate layer

                    if (be instanceof MobSpawnerBlockEntity) {
                        result.hasSpawner = true;
                    } else if (isStorage(be)) {
                        result.hasStorage = true;
                        result.storageCount++;
                    }
                }

                // Only mark if it has something interesting
                if (result.hasSpawner || (result.hasStorage && result.storageCount >= minStorageCount.get())) {
                    results.put(cpos.toLong(), result);
                }
            }
        }
    }

    private boolean isStorage(BlockEntity be) {
        return be instanceof ChestBlockEntity
            || be instanceof TrappedChestBlockEntity
            || be instanceof BarrelBlockEntity
            || be instanceof ShulkerBoxBlockEntity
            || be instanceof HopperBlockEntity
            || be instanceof DropperBlockEntity
            || be instanceof DispenserBlockEntity
            || be instanceof FurnaceBlockEntity
            || be instanceof BlastFurnaceBlockEntity
            || be instanceof SmokerBlockEntity;
    }

    private void renderResults(Render3DEvent event) {
        for (ChunkResult result : results.values()) {
            int worldX = result.pos.getStartX();
            int worldZ = result.pos.getStartZ();

            // Draw chunk highlight at Y=-32 (middle of deepslate zone)
            // Chunk is 16x16 blocks
            Color color = result.hasSpawner ? spawnerChunkColor.get() : storageChunkColor.get();

            // Box from bottom of deepslate to Y=0
            event.renderer.box(
                worldX, -64, worldZ,
                worldX + 16, 0, worldZ + 16,
                color, color,
                shapeMode.get(), 0
            );
        }
    }
}
