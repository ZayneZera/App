# Coords Display (Fabric Mod, Minecraft 1.16.1)

Zeigt permanent die X/Y/Z-Koordinaten des Spielers oben mittig am Bildschirmrand an — kein Toggle, kein Keybind, einfach immer sichtbar (respektiert nur F1 "GUI ausblenden").

## Bauen

Voraussetzungen: JDK 8, Internetzugriff (lädt Minecraft/Yarn/Fabric-Artefakte herunter). Der Gradle Wrapper ist bereits im Projekt enthalten, du brauchst kein separates Gradle zu installieren.

Windows (in `fabric-coords-hud`):

```powershell
.\gradlew.bat build
```

macOS/Linux:

```bash
cd fabric-coords-hud
./gradlew build
```

Die fertige `.jar` liegt danach in `build/libs/` und kommt in den `mods`-Ordner einer Fabric-1.16.1-Installation (Fabric Loader + Fabric API erforderlich).

## Hinweis zu den Versionsnummern

Die Versionen in `gradle.properties` (Yarn-Mappings, Fabric Loader, Fabric API) entsprechen den zur Minecraft-1.16.1-Zeit gängigen Ständen. Netzwerkzugriff auf `maven.fabricmc.net` war in dieser Umgebung nicht möglich, um sie live zu verifizieren. Falls Gradle beim Auflösen der Abhängigkeiten fehlschlägt, auf https://fabricmc.net/develop die aktuell für 1.16.1 passenden Versionen prüfen und in `gradle.properties` anpassen.

## Code

- `CoordsDisplayMod.java` – Client-Entrypoint, registriert den HUD-Renderer.
- `CoordsHudRenderer.java` – zeichnet den Text via `HudRenderCallback` jeden Frame oben mittig.
