package dev.polymixin.core.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class DevModFolders {

    private static final String ENV = "MOD_CLASSES";
    private static final String PROP_FOLDERS = "fml.modFolders";
    private static final String PROP_FOLDERS_FILE = "fml.modFoldersFile";
    private static final String SEPARATOR = "%%";

    private DevModFolders() {
    }

    public static List<Path> paths() {
        List<Path> paths = new ArrayList<>();
        addEntries(paths, System.getenv(ENV));
        addEntries(paths, System.getProperty(PROP_FOLDERS));
        addFromFile(paths, System.getProperty(PROP_FOLDERS_FILE));
        return paths;
    }

    private static void addEntries(List<Path> paths, String raw) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        for (String entry : raw.split(File.pathSeparator)) {
            if (entry.isEmpty()) {
                continue;
            }
            int marker = entry.indexOf(SEPARATOR);
            addPath(paths, marker < 0 ? entry : entry.substring(marker + SEPARATOR.length()));
        }
    }

    private static void addFromFile(List<Path> paths, String file) {
        if (file == null || file.isEmpty()) {
            return;
        }
        Properties properties = new Properties();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(file))) {
            properties.load(reader);
        } catch (IOException | RuntimeException ex) {
            return;
        }
        for (String name : properties.stringPropertyNames()) {
            String value = properties.getProperty(name);
            if (value == null) {
                continue;
            }
            for (String entry : value.split(File.pathSeparator)) {
                addPath(paths, entry);
            }
        }
    }

    private static void addPath(List<Path> paths, String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        try {
            Path path = Paths.get(trimmed);
            if (Files.exists(path)) {
                paths.add(path);
            }
        } catch (Throwable ignored) {
            // not a usable path
        }
    }
}
