package com.auradebug.hud;

import com.auradebug.AuraDebugAddon;
import com.auradebug.modules.StaffRadarModule;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import java.util.List;

public class StaffRadarHud extends HudElement {

    public static final HudElementInfo<StaffRadarHud> INFO =
        new HudElementInfo<>(AuraDebugAddon.HUD_GROUP, "staff-radar-hud",
            "Zeigt online Staff-Mitglieder als verschiebbares HUD-Element an.",
            StaffRadarHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SettingColor> titleColor = sgGeneral.add(new ColorSetting.Builder()
        .name("title-color")
        .description("Farbe des Titels.")
        .defaultValue(new SettingColor(255, 80, 80, 255))
        .build()
    );

    private final Setting<SettingColor> staffOnlineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("staff-online-color")
        .description("Farbe wenn Staff in der Nähe ist.")
        .defaultValue(new SettingColor(255, 50, 50, 255))
        .build()
    );

    private final Setting<SettingColor> staffFarColor = sgGeneral.add(new ColorSetting.Builder()
        .name("staff-far-color")
        .description("Farbe wenn Staff nicht in Render-Distanz ist.")
        .defaultValue(new SettingColor(200, 200, 200, 255))
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Schriftgröße des HUD-Elements.")
        .defaultValue(1.0)
        .min(0.5)
        .sliderMax(3.0)
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color")
        .description("Hintergrundfarbe des HUD-Elements.")
        .defaultValue(new SettingColor(0, 0, 0, 120))
        .build()
    );

    public StaffRadarHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        StaffRadarModule module = Modules.get().get(StaffRadarModule.class);
        double s = scale.get();

        double x = this.x;
        double y = this.y;

        double lineH = renderer.textHeight(false) * s + 2;
        List<StaffRadarModule.StaffEntry> staff = (module != null && module.isActive())
            ? module.onlineStaff
            : List.of();

        double boxW = 170 * s;
        double boxH = lineH * (staff.size() + 1) + 6;

        renderer.quad(x, y, boxW, boxH, backgroundColor.get());
        renderer.text("§l⚠ Staff Radar", x + 4, y + 3, titleColor.get(), false, s);
        double textY = y + lineH + 3;

        if (staff.isEmpty()) {
            renderer.text("§7Kein Staff online", x + 4, textY, staffFarColor.get(), false, s);
        } else {
            for (StaffRadarModule.StaffEntry entry : staff) {
                String text = entry.isNearby
                    ? "§c● " + entry.name + " §7(" + (int) entry.distance + "m)"
                    : "§7○ " + entry.name + " §8(nicht sichtbar)";
                SettingColor color = entry.isNearby ? staffOnlineColor.get() : staffFarColor.get();
                renderer.text(text, x + 4, textY, color, false, s);
                textY += lineH;
            }
        }

        setSize(boxW, boxH);
    }
}
