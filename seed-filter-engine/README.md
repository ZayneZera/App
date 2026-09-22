# Seed Filter Engine (external, native)

Headless native search backend for the seed-filter mod. Uses
[cubiomes](https://github.com/Cubitect/cubiomes) (MIT-licensed, vendored under
`third_party/cubiomes/`) directly for structure placement and spawn lookup - no hand-reimplemented
RNG math, so it's immune to the class of bugs we spent a long time chasing in the Java version.

All settings UI now lives IN-GAME (Ctrl+M in the mod, see `fabric-seed-filter/`) rather than in
this exe - the mod writes `seedfilter.cfg` and this exe just reads it and searches. There used to
also be a Win32 GUI mode (`src/gui_win32.c`) for configuring settings in a standalone window; it's
no longer built/shipped since the in-game menu replaced it, but the source is left in place in
case it's useful again later.

## Usage

The Minecraft mod runs `seedfilter.exe --search <config path>` itself and reads the seed back from
stdout ("Seed: ...", "SpawnX: ...", "SpawnZ: ...", "Cheats: 0|1"). This is what the title-screen
boots button and the pause-menu "Nächster Seed" button call - there's nothing to run manually.

## Building

Requires a C compiler (MinGW-w64 on Windows, or cross-compile from Linux):

```
x86_64-w64-mingw32-gcc -O2 -std=gnu11 -Isrc -Ithird_party/cubiomes \
  src/config.c src/engine.c src/search.c src/main_cli.c src/loot.c src/frame.c \
  third_party/cubiomes/{biomenoise,biomes,finders,generator,layers,noise,quadbase,util}.c \
  -lm -static -lwinpthread -static-libgcc -o seedfilter.exe   # Windows, statically linked
```

(Drop the `x86_64-w64-mingw32-` prefix and swap `-static -lwinpthread -static-libgcc` for
`-lpthread` to build a native Linux binary for local testing instead.)

## Layout

- `src/config.c/h` - plain-text config load/save (`key=value` lines).
- `src/engine.c/h` - criteria checking against a single seed, using cubiomes' `getStructurePos`,
  `isViableStructurePos`, `getVariant` (bastion type) and `getSpawn` directly.
- `src/search.c` - multithreaded random-seed scanning until a match or cancel.
- `src/mc_random.h` - vanilla chunk/decorator/position-hash seed derivation helpers (verified
  against decompiled 1.16.1 source), built on cubiomes' Java-Random primitives in `rng.h`.
- `src/loot.c/h` - resolves specific structure chest loot table criteria (buried treasure's
  TNT/diamond/iron/fish counts, ruined portal's obsidian/flint&steel/fire-charge/golden-axe
  counts) from the world seed and the structure's chunk position.
- `src/frame.c/h` - approximate Ruined Portal frame-completability check (Air/Obsidian at the
  border positions, no Crying Obsidian). Only handles 7 of the 13 portal templates (the others
  have non-standard or unclear geometry) and uses cubiomes' approximate terrain height in place of
  real block-level terrain generation - see the caveats documented in frame.h.
- `src/main_cli.c` - headless CLI entry point (any platform) - this is what actually ships.
- `src/gui_win32.c` - retired standalone Win32 settings GUI, not currently built.
