# Coords Display (Fabric Mod, Minecraft 1.16.1)

Zeigt permanent die X/Y/Z-Koordinaten des Spielers oben mittig am Bildschirmrand an — kein Toggle, kein Keybind, einfach immer sichtbar (respektiert nur F1 "GUI ausblenden").

## Bauen

Voraussetzungen: ein aktuelles JDK (17+; getestet mit Java 21), Internetzugriff (lädt Gradle/Minecraft/Yarn/Fabric-Artefakte herunter). Der Gradle Wrapper ist bereits im Projekt enthalten, du brauchst kein separates Gradle zu installieren. JAVA_HOME muss normalerweise nicht gesetzt werden.

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

Das Projekt nutzt bewusst ein aktuelles Gradle (8.10.2) und aktuelles Fabric Loom (1.9.2), obwohl die Mod für die alte Minecraft-Version 1.16.1 gebaut wird — das Build-Tool läuft dadurch stabil auf modernen JDKs, während der erzeugte Mod-Bytecode per `release = 8` trotzdem auf Java 8 (Minecraft-1.16.1-kompatibel) eingestellt bleibt.

## Hinweis zu den Versionsnummern

Yarn-Mappings, Fabric Loader und Fabric API in `gradle.properties` wurden gegen die echte Verzeichnisliste auf maven.fabricmc.net geprüft und sind für Minecraft 1.16.1 bestätigt korrekt.

## Code

- `CoordsDisplayMod.java` – Client-Entrypoint, registriert den HUD-Renderer.
- `CoordsHudRenderer.java` – zeichnet den Text via `HudRenderCallback` jeden Frame oben mittig.
