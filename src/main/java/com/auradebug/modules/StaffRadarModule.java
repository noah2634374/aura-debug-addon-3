package com.auradebug.modules;

import com.auradebug.AuraDebugAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.*;

public class StaffRadarModule extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> staffNames = sgGeneral.add(new StringListSetting.Builder()
        .name("staff-names")
        .description("Liste der Staff-Namen die überwacht werden sollen.")
        .defaultValue(Arrays.asList(
            "Notch", "jeb_", "Dream", "Admin", "Moderator"
        ))
        .build()
    );

    private final Setting<Boolean> showDistance = sgGeneral.add(new BoolSetting.Builder()
        .name("show-distance")
        .description("Zeigt die Distanz zum Staff-Spieler an.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> chatAlert = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-alert")
        .description("Sendet eine Chat-Nachricht wenn ein Staff online geht.")
        .defaultValue(true)
        .build()
    );

    private final Set<String> previousOnlineStaff = new HashSet<>();
    public final List<StaffEntry> onlineStaff = new ArrayList<>();

    public static class StaffEntry {
        public String name;
        public double distance;
        public boolean isNearby;

        public StaffEntry(String name, double distance, boolean isNearby) {
            this.name = name;
            this.distance = distance;
            this.isNearby = isNearby;
        }
    }

    public StaffRadarModule() {
        super(AuraDebugAddon.CATEGORY, "staff-radar",
            "Zeigt alle online Staff-Mitglieder mit Distanz an. Konfigurierbar via HUD.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return;

        onlineStaff.clear();
        Set<String> currentStaff = new HashSet<>();
        List<String> staffList = staffNames.get();

        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().getName();
            for (String staffName : staffList) {
                if (name.equalsIgnoreCase(staffName)) {
                    currentStaff.add(name);

                    double dist = -1;
                    boolean nearby = false;
                    // Try to find the player entity for distance
                    for (var player : mc.world.getPlayers()) {
                        if (player.getName().getString().equalsIgnoreCase(name)) {
                            dist = mc.player.distanceTo(player);
                            nearby = true;
                            break;
                        }
                    }

                    onlineStaff.add(new StaffEntry(name, dist, nearby));

                    if (!previousOnlineStaff.contains(name) && chatAlert.get()) {
                        mc.player.sendMessage(Text.of(
                            "§c[AuraDebug] §eStaff online: §f" + name
                        ), false);
                    }
                    break;
                }
            }
        }

        // Offline alert
        for (String prev : previousOnlineStaff) {
            if (!currentStaff.contains(prev) && chatAlert.get()) {
                mc.player.sendMessage(Text.of(
                    "§c[AuraDebug] §7Staff offline: §f" + prev
                ), false);
            }
        }

        previousOnlineStaff.clear();
        previousOnlineStaff.addAll(currentStaff);
    }

    public boolean isShowDistance() {
        return showDistance.get();
    }

    public List<String> getStaffNames() {
        return staffNames.get();
    }
}
