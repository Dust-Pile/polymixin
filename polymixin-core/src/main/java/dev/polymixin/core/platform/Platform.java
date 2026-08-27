package dev.polymixin.core.platform;

import dev.polymixin.core.diagnostics.Log;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Platform {

    public static final String PROP_SOURCE = "polymixin.classpathSource";
    public static final String PROP_DEBUG = "polymixin.debug";

    private static final String[] CANDIDATES = {
            "dev.polymixin.forge.ForgeClasspathSource",
            "dev.polymixin.neoforge.NeoForgeClasspathSource",
            "dev.polymixin.fabric.FabricClasspathSource"
    };

    private static ClasspathSource override;
    private static ClasspathSource resolved;
    private static boolean resolvedOnce;

    private Platform() {
    }

    public static synchronized void override(ClasspathSource source) {
        override = source;
        resolved = null;
        resolvedOnce = false;
    }

    public static synchronized ClasspathSource source() {
        if (override != null) {
            return override;
        }
        if (resolvedOnce) {
            return resolved;
        }
        resolvedOnce = true;

        List<String> names = new ArrayList<>();
        String custom = System.getProperty(PROP_SOURCE);
        if (custom != null && !custom.isEmpty()) {
            names.add(custom);
        }
        for (String candidate : CANDIDATES) {
            names.add(candidate);
        }

        for (String name : names) {
            try {
                Class<?> type = Class.forName(name, true, Platform.class.getClassLoader());
                ClasspathSource candidate = (ClasspathSource) type.getDeclaredConstructor().newInstance();
                if (candidate.isAvailable()) {
                    resolved = candidate;
                    return resolved;
                }
            } catch (ClassNotFoundException | LinkageError ignored) {
                continue;
            } catch (Throwable th) {
                Log.warn("classpath source {} failed to initialise: {}", name, th);
            }
        }
        return null;
    }

    public static List<Path> roots() {
        ClasspathSource source = source();
        if (source == null) {
            return List.of();
        }
        Set<Path> unique = new LinkedHashSet<>(source.classpathRoots());
        List<Path> normalized = ClasspathRoots.normalize(unique);
        if (Boolean.getBoolean(PROP_DEBUG)) {
            for (Path root : normalized) {
                Log.info("  root {} [{}]", root, root.getFileSystem().provider().getScheme());
            }
        }
        return normalized;
    }
}
