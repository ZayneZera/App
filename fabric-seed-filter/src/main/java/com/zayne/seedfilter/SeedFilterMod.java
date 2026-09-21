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
            applyCreativeDefault(accessor);
        }

        accessor.invokeCreateLevel();
        announceComputedSpawn(client, result.spawnX, result.spawnZ);
    }

    /**
     * Sets CreateWorldScreen's "currentMode" (the Survival/Creative/Hardcore selector, read at
     * createLevel() time to pick the actual world game mode) to Creative.
     *
     * currentMode's real type is CreateWorldScreen's own package-private "Mode" enum, which our
     * mod's package can't reference as a Java type at all (confirmed by a real compile error:
     * "Mode is not public in CreateWorldScreen"). Reflection sidesteps that entirely - once we
     * have a live Mode instance (via the Object-typed accessor), getClass() gives us its real
     * runtime Class regardless of source-level visibility, and getEnumConstants()/setAccessible
     * work on it the same way they would for any enum.
     */
    private static void applyCreativeDefault(CreateWorldScreenAccessor accessor) {
        try {
            Object currentMode = accessor.getCurrentMode();
            Class<?> modeClass = currentMode.getClass();
            Field defaultGameModeField = modeClass.getDeclaredField("defaultGameMode");
            defaultGameModeField.setAccessible(true);

            for (Object mode : modeClass.getEnumConstants()) {
                if (defaultGameModeField.get(mode) == GameMode.CREATIVE) {
                    accessor.setCurrentMode(mode);
                    return;
                }
            }
            LOGGER.warn("Could not find a Creative CreateWorldScreen.Mode entry");
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Failed to set Creative game mode default", e);
        }
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
