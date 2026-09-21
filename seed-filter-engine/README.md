# Seed Filter Engine (external, native)

Standalone Windows tool that replaces the mod's in-game settings screen and Java-side seed
scanning. Uses [cubiomes](https://github.com/Cubitect/cubiomes) (MIT-licensed, vendored under
`third_party/cubiomes/`) directly for structure placement and spawn lookup - no hand-reimplemented
RNG math, so it's immune to the class of bugs we spent a long time chasing in the Java version.

## Usage

- Run `seedfilter.exe` with no arguments to open the settings window (sliders/checkboxes for every
  criterion, mirrors the old in-mod settings screen). Click "Speichern" to save, or "Suche starten"
  to run a search right there and see the result.
- The Minecraft mod runs `seedfilter.exe --search <config path>` itself (no window opens) and reads
  the seed back from stdout. This is what the title-screen boots button and the pause-menu
  "Nächster Seed" button call.
- Settings are saved to `seedfilter.cfg` (plain `key=value` text) next to the exe by default; the
  Minecraft mod points it at a copy inside the game directory so both the GUI and the mod agree on
  the same file.

## Building

Requires a C compiler (MinGW-w64 on Windows, or cross-compile from Linux):

```
gcc -O2 -std=gnu11 -Isrc -Ithird_party/cubiomes \
  src/config.c src/engine.c src/search.c src/main_cli.c \
  third_party/cubiomes/{biomenoise,biomes,finders,generator,layers,noise,quadbase,util}.c \
  -lm -lpthread -o seedfilter_cli   # headless CLI only, any platform

x86_64-w64-mingw32-gcc -O2 -std=gnu11 -mwindows -Isrc -Ithird_party/cubiomes \
  src/config.c src/engine.c src/search.c src/gui_win32.c \
  third_party/cubiomes/{biomenoise,biomes,finders,generator,layers,noise,quadbase,util}.c \
  -lm -static -lwinpthread -lcomctl32 -static-libgcc -o seedfilter.exe   # Windows GUI + headless
```

## Layout

- `src/config.c/h` - plain-text config load/save (`key=value` lines).
- `src/engine.c/h` - criteria checking against a single seed, using cubiomes' `getStructurePos`,
  `isViableStructurePos`, `getVariant` (bastion type) and `getSpawn` directly.
- `src/search.c` - multithreaded random-seed scanning until a match or cancel.
- `src/main_cli.c` - headless-only CLI entry point (any platform, no GUI dependency).
- `src/gui_win32.c` - Win32 GUI entry point; also handles `--search` for the mod's headless calls
  so one exe does both jobs on Windows.
