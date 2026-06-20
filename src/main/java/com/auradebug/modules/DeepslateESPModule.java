package com.auradebug.modules;

import com.auradebug.AuraDebugAddon;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

public class DeepslateESPModule extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("scan-radius")
        .description("Radius in Blöcken der nach Ores unter Deepslate durchsucht wird.")
        .defaultValue(32)
        .min(8)
        .sliderMax(64)
        .build()
    );

    private final Setting<SettingColor> oreColor = sgGeneral.add(new ColorSetting.Builder()
        .name("ore-color")
        .description("Farbe für Erze unter Deepslate.")
        .defaultValue(new SettingColor(255, 200, 0, 180))
        .build()
    );

    private final Setting<SettingColor> spawnerColor = sgGeneral.add(new ColorSetting.Builder()
        .name("spawner-color")
        .description("Farbe für Spawner unter Deepslate.")
        .defaultValue(new SettingColor(255, 0, 0, 200))
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("Wie die Blöcke gerendert werden.")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    // Deepslate starts around Y=0 and below, but technically Y<=-8 is all deepslate
    // We scan everything below deepslate layer (Y < 0)
    private static final int DEEPSLATE_Y_THRESHOLD = 0;

    private final List<BlockPos> foundBlocks = new ArrayList<>();
    private int scanTick = 0;

    public DeepslateESPModule() {
        super(AuraDebugAddon.CATEGORY, "deepslate-esp",
            "Zeigt Blöcke (Erze, Spawner) unter dem Deepslate-Layer durch Wände hindurch an.");
    }

    @Override
    public void onActivate() {
        foundBlocks.clear();
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        // Rescan every 40 ticks
        scanTick++;
        if (scanTick >= 40) {
            scanTick = 0;
            scan();
        }

        // Render found blocks
        for (BlockPos pos : foundBlocks) {
            BlockState state = mc.world.getBlockState(pos);
            Block block = state.getBlock();

            SettingColor color = (block == Blocks.SPAWNER) ? spawnerColor.get() : oreColor.get();

            event.renderer.box(pos, color, color, shapeMode.get(), 0);
        }
    }

    private void scan() {
        if (mc.player == null || mc.world == null) return;
        foundBlocks.clear();

        BlockPos playerPos = mc.player.getBlockPos();
        int r = radius.get();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                // Scan below Y=0 (deepslate zone, down to bedrock at Y=-64)
                for (int y = -64; y <= DEEPSLATE_Y_THRESHOLD; y++) {
                    BlockPos pos = playerPos.add(x, y - playerPos.getY(), z);
                    BlockState state = mc.world.getBlockState(pos);
                    Block block = state.getBlock();

                    if (isInteresting(block)) {
                        foundBlocks.add(pos.toImmutable());
                    }
                }
            }
        }
    }

    private boolean isInteresting(Block block) {
        return block == Blocks.SPAWNER
            || block == Blocks.DIAMOND_ORE
            || block == Blocks.DEEPSLATE_DIAMOND_ORE
            || block == Blocks.ANCIENT_DEBRIS
            || block == Blocks.DEEPSLATE_GOLD_ORE
            || block == Blocks.DEEPSLATE_IRON_ORE
            || block == Blocks.DEEPSLATE_COAL_ORE
            || block == Blocks.DEEPSLATE_LAPIS_ORE
            || block == Blocks.DEEPSLATE_REDSTONE_ORE
            || block == Blocks.DEEPSLATE_COPPER_ORE
            || block == Blocks.DEEPSLATE_EMERALD_ORE
            || block == Blocks.CHEST
            || block == Blocks.TRAPPED_CHEST
            || block == Blocks.BARREL
            || block == Blocks.SHULKER_BOX;
    }
}
