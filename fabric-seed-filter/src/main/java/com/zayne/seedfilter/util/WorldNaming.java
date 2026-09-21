package com.zayne.seedfilter.util;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class WorldNaming {
    private static final Pattern CF_PATTERN = Pattern.compile("CF(\\d+)");

    public static String nextName() {
        Path savesDir = FabricLoader.getInstance().getGameDir().resolve("saves");
        int max = 0;
        if (Files.isDirectory(savesDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(savesDir)) {
                for (Path p : stream) {
                    String name = p.getFileName().toString();
                    java.util.regex.Matcher matcher = CF_PATTERN.matcher(name);
                    if (matcher.matches()) {
                        int n = Integer.parseInt(matcher.group(1));
                        if (n > max) {
                            max = n;
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return "CF" + (max + 1);
    }
}
