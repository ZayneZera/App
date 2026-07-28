# App-Lock (privat, Samsung S25 Ultra / OneUI 8.5)

Native Android-App (Kotlin, Jetpack Compose), die einzelne Apps mit Fingerabdruck/PIN
sperrt. Läuft rein event-basiert über einen Accessibility-Dienst (kein Polling), damit
der Akkuverbrauch minimal bleibt.

## Funktionen (v0.1 – erster Entwurf)

- App-Auswahl: beliebige installierte App sperren/entsperren
- Entsperrung per Fingerabdruck (BiometricPrompt) mit PIN-Fallback, pro App oder global
  auf "nur PIN" umstellbar
- Einstellbare Zeit, nach der eine App nach Verlassen wieder gesperrt wird (global und
  pro App überschreibbar)
- Task-Manager-Schutz: blockiert den Task-Switcher, wenn die zuletzt genutzte App
  gesperrt war
- Deinstallationsschutz über Geräteadministrator-Rechte
- Fest eingebaute Sicherheits-Ausnahmen (eigener Launcher, Einstellungen,
  Standard-Telefon-App), damit man sich nicht selbst aussperrt
- Eigene PIN schützt zusätzlich die App-Einstellungen selbst

## APK bauen und installieren

Diese Sandbox hat kein Android SDK und keinen Netzwerkzugriff auf Googles Server, daher
baut GitHub Actions die APK automatisch bei jedem Push (`.github/workflows/build-apk.yml`):

1. Auf GitHub zum Branch `claude/app-lock-fingerprint-n16bpi` wechseln -> Tab **Actions**
2. Neuesten Workflow-Lauf **"Build APK"** öffnen
3. Unter **Artifacts** `app-lock-debug-apk` herunterladen (ZIP), entpacken -> `app-debug.apk`
4. APK aufs Handy übertragen und installieren (Installation aus unbekannten Quellen /
   "Diese Quelle zulassen" muss einmalig erlaubt werden)

## Einrichtung nach der Installation

1. App öffnen, 6-stellige PIN festlegen (Fallback, falls Fingerabdruck fehlschlägt)
2. Bedienungshilfen-Dienst aktivieren (Einstellungen -> Bedienungshilfen -> App-Lock).
   Falls der Schalter ausgegraut ist: App-Info -> Drei-Punkte-Menü -> **"Eingeschränkte
   Einstellungen zulassen"** (Android blockiert das bei Apps außerhalb des Play Store
   standardmäßig)
3. Akku-Optimierung für die App deaktivieren (sonst killt Samsung den Dienst irgendwann
   im Hintergrund und die Sperre reagiert nicht mehr zuverlässig)
4. Optional: Geräteadministrator aktivieren (Einstellungen -> Deinstallationsschutz)

## Bekannte Einschränkungen

- Kein echter Root-/Systemschutz: Geräteadministrator verhindert die direkte
  Deinstallation, kann aber über die Systemeinstellungen deaktiviert werden.
- Die Erkennung des Task-Switchers (`LockAccessibilityService.looksLikeRecentsScreen`)
  ist eine Heuristik. Falls der Task-Manager-Schutz auf dem S25U (OneUI 8.5) nicht
  auslöst, per `adb shell dumpsys window windows | grep -i recent` die tatsächliche
  Fenster-Klasse prüfen und in `LockAccessibilityService.kt` ergänzen.
- Erste Version, ungetestet auf echter Hardware (diese Sandbox hat keinen Android-Emulator).
  Feinschliff/Bugfixing nach erstem Testlauf auf dem Gerät ist zu erwarten.
