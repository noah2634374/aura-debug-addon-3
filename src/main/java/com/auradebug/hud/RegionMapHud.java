package com.auradebug.hud;

import com.auradebug.AuraDebugAddon;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class RegionMapHud extends HudElement {

    public static final HudElementInfo<RegionMapHud> INFO =
        new HudElementInfo<>(AuraDebugAddon.HUD_GROUP, "region-map-hud",
            "Zeigt die aktuelle Region, Chunk und Koordinaten auf der Karte an.",
            RegionMapHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> mapSize = sgGeneral.add(new IntSetting.Builder()
        .name("map-size")
        .description("Größe der Karte in Pixeln.")
        .defaultValue(120)
        .min(60)
        .sliderMax(300)
        .build()
    );

    private final Setting<Integer> chunkRange = sgGeneral.add(new IntSetting.Builder()
        .name("chunk-range")
        .description("Wie viele Chunks die Karte anzeigt.")
        .defaultValue(8)
        .min(4)
        .sliderMax(32)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color")
        .defaultValue(new SettingColor(20, 20, 20, 180))
        .build()
    );

    private final Setting<SettingColor> gridColor = sgGeneral.add(new ColorSetting.Builder()
        .name("grid-color")
        .description("Farbe des Chunk-Rasters.")
        .defaultValue(new SettingColor(60, 60, 60, 180))
        .build()
    );

    private final Setting<SettingColor> playerColor = sgGeneral.add(new ColorSetting.Builder()
        .name("player-color")
        .description("Farbe des Spieler-Markers.")
        .defaultValue(new SettingColor(255, 255, 0, 255))
        .build()
    );

    private final Setting<SettingColor> regionBorderColor = sgGeneral.add(new ColorSetting.Builder()
        .name("region-border-color")
        .description("Farbe der Region-Grenze (512 Blöcke).")
        .defaultValue(new SettingColor(255, 100, 0, 200))
        .build()
    );

    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("text-color")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    public RegionMapHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        int size = mapSize.get();
        int range = chunkRange.get();
        double x = this.x;
        double y = this.y;

        // Background
        renderer.quad(x, y, size + 4, size + 40, backgroundColor.get());

        double mapX = x + 2;
        double mapY = y + 2;

        // Draw map background
        renderer.quad(mapX, mapY, size, size, new SettingColor(30, 50, 30, 200));

        BlockPos playerPos = mc.player.getBlockPos();
        ChunkPos playerChunk = mc.player.getChunkPos();

        // Draw chunk grid
        double chunkPixels = (double) size / (range * 2);
        for (int cx = -range; cx <= range; cx++) {
            double lineX = mapX + (cx + range) * chunkPixels;
            renderer.line(lineX, mapY, lineX, mapY + size, gridColor.get());
        }
        for (int cz = -range; cz <= range; cz++) {
            double lineZ = mapY + (cz + range) * chunkPixels;
            renderer.line(mapX, lineZ, mapX + size, lineZ, gridColor.get());
        }

        // Draw region borders (every 512 blocks = 32 chunks)
        int regionX = Math.floorMod(playerChunk.x, 32);
        int regionZ = Math.floorMod(playerChunk.z, 32);

        // West region border
        double borderXw = mapX + (range - regionX) * chunkPixels;
        if (borderXw >= mapX && borderXw <= mapX + size)
            renderer.line(borderXw, mapY, borderXw, mapY + size, regionBorderColor.get());

        // East region border
        double borderXe = mapX + (range - regionX + 32) * chunkPixels;
        if (borderXe >= mapX && borderXe <= mapX + size)
            renderer.line(borderXe, mapY, borderXe, mapY + size, regionBorderColor.get());

        // North region border
        double borderZn = mapY + (range - regionZ) * chunkPixels;
        if (borderZn >= mapY && borderZn <= mapY + size)
            renderer.line(mapX, borderZn, mapX + size, borderZn, regionBorderColor.get());

        // South region border
        double borderZs = mapY + (range - regionZ + 32) * chunkPixels;
        if (borderZs >= mapY && borderZs <= mapY + size)
            renderer.line(mapX, borderZs, mapX + size, borderZs, regionBorderColor.get());

        // Draw player marker (center)
        double pxMap = mapX + size / 2.0 - 3;
        double pyMap = mapY + size / 2.0 - 3;
        renderer.quad(pxMap, pyMap, 6, 6, playerColor.get());

        // Draw other players
        for (var player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            int dx = player.getChunkPos().x - playerChunk.x;
            int dz = player.getChunkPos().z - playerChunk.z;
            if (Math.abs(dx) > range || Math.abs(dz) > range) continue;

            double ppx = mapX + (range + dx) * chunkPixels + chunkPixels / 2;
            double ppz = mapY + (range + dz) * chunkPixels + chunkPixels / 2;
            renderer.quad(ppx - 2, ppz - 2, 4, 4, new SettingColor(100, 180, 255, 220));
        }

        // Text info below map
        int regionFileX = playerPos.getX() >> 9;
        int regionFileZ = playerPos.getZ() >> 9;
        String regionName = "r." + regionFileX + "." + regionFileZ;

        renderer.text("§lRegion: §f" + regionName, mapX, mapY + size + 3, textColor.get(), false, 0.85);
        renderer.text("§7Chunk: §f" + playerChunk.x + ", " + playerChunk.z,
            mapX, mapY + size + 14, textColor.get(), false, 0.85);
        renderer.text("§7XYZ: §f" + playerPos.getX() + " " + playerPos.getY() + " " + playerPos.getZ(),
            mapX, mapY + size + 25, textColor.get(), false, 0.85);

        setSize(size + 4, size + 40);
    }
}
