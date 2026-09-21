package com.zayne.seedfilter;

import com.zayne.seedfilter.gui.EngineMissingScreen;
import com.zayne.seedfilter.gui.ScanProgressScreen;
import com.zayne.seedfilter.mixin.CreateWorldScreenAccessor;
import com.zayne.seedfilter.mixin.MoreOptionsDialogAccessor;
import com.zayne.seedfilter.util.WorldNaming;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.MoreOptionsDialog;
import net.minecraft.text.LiteralText;
import net.minecraft.world.GameMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

/**
 * All criteria filtering now lives in the external seedfilter.exe (see seed-filter-engine/ in
 * the repo) - this mod is just the in-game trigger + world-creation glue around it.
 */
public class SeedFilterMod implements ClientModInitializer {

    public static final Logger LOGGER = LogManager.getLogger("seed-filter");

    @Override
    public void onInitializeClient() {
        MenuHotkey.register();
    }

    public static void startScanAndCreate(MinecraftClient client, Screen titleScreen) {
        if (!ExternalEngine.isInstalled()) {
            client.openScreen(new EngineMissingScreen(titleScreen));
            return;
        }

        AtomicReference<Process> processHolder = new AtomicReference<>();
        EngineStats stats = new EngineStats();
        client.openScreen(new ScanProgressScreen(titleScreen, processHolder, stats));

        ExternalEngine.runAsync(processHolder, stats).thenAccept(result ->
                client.execute(() -> createAndJoin(client, titleScreen, result))
        ).exceptionally(error -> {
            LOGGER.warn("Seed search did not produce a result", error);
            return null;
        });
    }

    public static void createAndJoin(MinecraftClient client, Screen titleScreen, ExternalEngine.Result result) {
        CreateWorldScreen screen = new CreateWorldScreen(titleScreen);
        client.openScreen(screen);

        CreateWorldScreenAccessor accessor = (CreateWorldScreenAccessor) screen;
        accessor.getLevelNameField().setText(WorldNaming.nextName());

        MoreOptionsDialog dialog = accessor.getMoreOptionsDialog();
        ((MoreOptionsDialogAccessor) dialog).getSeedTextField().setText(String.valueOf(result.seed));

        if (result.cheats) {
            accessor.setCheatsEnabled(true);
        }

        if (result.creative) {
            applyCreativeDefault(screen);
        }

        accessor.invokeCreateLevel();
        announceComputedSpawn(client, result.spawnX, result.spawnZ);
    }

    /**
     * Sets CreateWorldScreen's Survival/Creative/Hardcore selector field to Creative, so
     * createLevel() picks Creative as the actual world game mode.
     *
     * That field's exact name turned out to be a moving target across Yarn mapping builds (our
     * project pins 1.16.1+build.21, but the name we looked up came from a later build and
     * didn't exist yet in build.21 - "Could not locate @Accessor target currentMode"). Rather
     * than keep guessing names per build, this scans CreateWorldScreen's own declared fields for
     * one whose TYPE is a nested enum declared inside CreateWorldScreen itself (there's only one
     * such field - the mode selector), then within that enum's constants finds whichever one
     * carries a field of type GameMode equal to GameMode.CREATIVE. Field/type *names* changing
     * between mapping builds doesn't affect this at all, since nothing here is looked up by name.
     */
    private static void applyCreativeDefault(CreateWorldScreen screen) {
        try {
            for (Field field : CreateWorldScreen.class.getDeclaredFields()) {
                Class<?> fieldType = field.getType();
                if (!fieldType.isEnum() || fieldType.getEnclosingClass() != CreateWorldScreen.class) {
                    continue;
                }
                Field gameModeField = findFieldOfType(fieldType, GameMode.class);
                if (gameModeField == null) {
                    continue;
                }
                gameModeField.setAccessible(true);
                for (Object candidate : fieldType.getEnumConstants()) {
                    if (gameModeField.get(candidate) == GameMode.CREATIVE) {
                        field.setAccessible(true);
                        field.set(screen, candidate);
                        return;
                    }
                }
            }
            LOGGER.warn("Could not find a Creative mode field on CreateWorldScreen via reflection");
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Failed to set Creative game mode default", e);
        }
    }

    private static Field findFieldOfType(Class<?> owner, Class<?> type) {
        for (Field f : owner.getDeclaredFields()) {
            if (f.getType() == type) {
                return f;
            }
        }
        return null;
    }

    /**
     * Polls (via self-requeuing client.execute) until the player has actually spawned in, then
     * posts the spawn position the engine computed - so it can be compared directly against F3
     * in-game.
     */
    private static void announceComputedSpawn(MinecraftClient client, int spawnX, int spawnZ) {
        client.execute(() -> {
            if (client.player == null) {
                announceComputedSpawn(client, spawnX, spawnZ);
                return;
            }
            client.player.sendMessage(new LiteralText(
                    "§e[SeedFilter] Berechneter Spawn: X=" + spawnX + " Z=" + spawnZ
                            + "  (mit F3 vergleichen)"), false);
        });
    }
}
