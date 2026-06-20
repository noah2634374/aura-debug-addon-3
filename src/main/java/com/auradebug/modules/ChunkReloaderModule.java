package com.auradebug.modules;

import com.auradebug.AuraDebugAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class ChunkReloaderModule extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> interval = sgGeneral.add(new IntSetting.Builder()
        .name("interval-ticks")
        .description("Wie viele Ticks zwischen jedem Chunk-Reload (20 Ticks = 1 Sekunde).")
        .defaultValue(200)
        .min(20)
        .sliderMax(1200)
        .build()
    );

    private int tickCounter = 0;

    public ChunkReloaderModule() {
        super(AuraDebugAddon.CATEGORY, "chunk-reloader",
            "Lädt alle sichtbaren Chunks periodisch neu – verhindert Ghost-Blocks und veraltete Chunk-Daten.");
    }

    @Override
    public void onActivate() {
        tickCounter = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        tickCounter++;
        if (tickCounter >= interval.get()) {
            tickCounter = 0;
            mc.worldRenderer.reload();
        }
    }
}
