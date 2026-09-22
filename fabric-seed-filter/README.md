# Speedrun Seed Filter (Fabric Mod, Minecraft 1.16.1)

Practice-Tool für Speedrunning: Titelbildschirm bekommt oben rechts einen Netherite-Boots-Button (Schnellstart) und einen Filter-Einstellungen-Button darunter.

**Aktueller Stand: Phase 1 (Grundgerüst)**

- [x] Einstellungs-Menü mit allen Kriterien (Ruined Portal, Village, Buried Treasure, Bastion inkl. Typ-Filter, Fortress) — Werte werden in `config/seed-filter.json` gespeichert
- [x] Netherite-Boots-Button + Filter-Button oben rechts im Hauptmenü
- [x] CF-Weltnamen-Logik vorbereitet (`WorldNaming.nextName()` scannt den `saves`-Ordner nach `CF1`, `CF2`, ... und findet die nächste freie Nummer)
- [ ] Boots-Button klickt aktuell nur "Neue Welt erstellen" auf (Vanilla-Screen) — noch **kein** automatisches Ausfüllen von Name/Seed und noch **kein** automatischer Klick auf "Erstellen". Das kommt in Phase 2, sobald wir die exakten internen Feldnamen des Erstellen-Screens kennen (dafür brauchen wir erst einen erfolgreichen Build).
- [ ] Echte Seed-Scan-Engine (Struktur-Distanz-Prüfung) — Phase 2
- [ ] Loot-Chest-Verifikation (Goldaxt, Obsidian-Anzahl, Schmied-Loot, TNT, Looting-Schwert) — Phase 3, per echter Hintergrund-Weltgenerierung (kein RNG-Nachbau, damit die Ergebnisse garantiert korrekt sind)

## Bauen

Gleicher moderne Toolchain wie beim Coords-Mod (Gradle 8.14.3 + Fabric Loom 1.9.2, läuft auf normalem Java 21, kein JAVA_HOME nötig):

```powershell
cd fabric-seed-filter
.\gradlew.bat build
```

`build` deployt die fertige jar jetzt automatisch direkt in den MCSR-PrismLauncher-`mods`-Ordner (siehe `deployToMcsrMods` in `build.gradle`) - kein manuelles Kopieren mehr nötig. Der vorher dort liegende Build wird dabei nicht einfach überschrieben, sondern vorher nach `build\libs\` verschoben (Rotations-Backup, eine Version tief).

## Bekannte Risiken für den ersten Build

Einige API-Namen (Item-Icon-Rendering `renderGuiItemIcon`, `addButton`/`addChild` für Widgets, `renderButton`-Override) sind aus der Erinnerung geschrieben und nicht 1:1 gegen die echten 1.16.1-Mappings geprüft. Falls der Build mit Kompilierfehlern (nicht Abstürzen!) fehlschlägt, einfach die Fehlermeldung schicken — das sind ungefährliche, schnell behebbare Fehler.
