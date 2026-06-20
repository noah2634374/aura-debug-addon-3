# Aura Debug – Meteor Addon für DonutSMP
**Minecraft 1.21.1 | Meteor Client 0.5.8-SNAPSHOT**

Alle Module erscheinen in der Kategorie **"Aura Debug"** im Meteor Client.

---

## Module

### 🔄 Chunk Reloader
- Lädt alle Chunks periodisch via `worldRenderer.reload()` neu
- **Setting:** `interval-ticks` (Standard: 200 Ticks = 10s)
- Verhindert Ghost-Blocks und veraltete Chunk-Daten

---

### 👤 Staff Radar
- Scannt die Spielerliste nach konfigurierbaren Staff-Namen
- Zeigt Distanz wenn der Staff-Spieler sichtbar (in Render-Distanz) ist
- Chat-Benachrichtigung wenn Staff online/offline geht
- **Settings:** `staff-names` (Liste), `show-distance`, `chat-alert`
- **HUD:** `staff-radar-hud` – verschiebbar, skalierbar, eigene Farben

---

### 🗺️ Region Map HUD
- Zeigt eine Mini-Karte mit:
  - Chunk-Raster
  - Orangen Region-Grenzen (512x512 Blöcke = 1 Region)
  - Gelber Spieler-Marker (Mitte)
  - Blaue Punkte für andere Spieler
  - Aktuelle Region-Datei (r.X.Z), Chunk-Koordinaten, XYZ
- **Settings:** `map-size`, `chunk-range`, diverse Farben
- **HUD:** `region-map-hud` – verschiebbar, skalierbar

---

### 💎 Deepslate ESP
- Scannt alle Blöcke unter Y=0 im einstellbaren Radius
- Zeigt Erze, Spawner, Kisten etc. durch Wände hindurch
- **Settings:** `scan-radius`, `ore-color`, `spawner-color`, `shape-mode`
- Rescan alle 40 Ticks automatisch

---

### 🔴 Spawner ESP
- Findet alle Spawner in konfigurierbarem Chunk-Radius
- Zeigt eine **rotierende rote Linie** so breit wie der Spawner (1 Block)
- Zeigt den Mob-Typ + Entfernung als Label über dem Spawner
- **Settings:** `line-color`, `line-height`, `search-radius`, `show-label`

---

### 🗺️ Chunk Finder
- Scannt Chunks auf Inhalte **unter dem Deepslate-Layer (Y < 0)**
- **Rot** = Chunk mit Spawner
- **Pink** = Chunk mit min. N Storage-Blöcken (Kisten, Fässer, etc.)
- Minimum Storage-Anzahl einstellbar (Standard: 5)
- **Settings:** `chunk-radius`, `min-storage-count`, beide Farben

---

## Installation / Build

### Voraussetzungen
- Java 21 (JDK)
- Minecraft 1.21.1 mit Fabric Loader ≥0.16.7
- Meteor Client 0.5.8-SNAPSHOT

### Bauen
```bash
./gradlew build
```
Die fertige JAR liegt in `build/libs/aura-debug-1.0.0.jar`.

### Installieren
1. JAR in `.minecraft/mods/` kopieren
2. Meteor Client muss bereits installiert sein
3. Minecraft starten → Kategorie **"Aura Debug"** im Meteor-Menü

---

## HUD-Elemente konfigurieren
1. Meteor-Menü öffnen (Standard: `Right Shift`)
2. → **HUD** Tab
3. Gruppe **"Aura Debug"** → `staff-radar-hud` oder `region-map-hud`
4. Aktivieren, dann per Drag & Drop verschieben
5. Größe via `scale`-Setting anpassen

---

## Hinweis
Dieses Addon ist für den privaten Gebrauch auf DonutSMP gedacht.
