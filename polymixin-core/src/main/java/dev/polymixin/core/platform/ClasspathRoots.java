package dev.polymixin.core.platform;

import dev.polymixin.core.diagnostics.Log;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ClasspathRoots {

    public static final String PROP_NO_RESOLVE = "polymixin.scan.noResolveRoots";
    public static final String PROP_NO_EXTRACT = "polymixin.scan.noExtractNested";

    private static final int MAX_DEPTH = 6;
    private static final long MAX_EXTRACT_BYTES = 256L * 1024L * 1024L;

    private ClasspathRoots() {
    }

    public static List<Path> normalize(Collection<Path> roots) {
        Set<Path> resolved = new LinkedHashSet<>();
        List<String> unscannable = new ArrayList<>();

        if (Boolean.getBoolean(PROP_NO_RESOLVE)) {
            resolved.addAll(roots);
            return new ArrayList<>(resolved);
        }

        for (Path root : roots) {
            resolve(root, 0, resolved, unscannable);
        }
        if (!unscannable.isEmpty()) {
            Log.warn("{} classpath root(s) could not be resolved to a scannable location and were skipped: {}",
                    unscannable.size(), unscannable);
        }
        return new ArrayList<>(resolved);
    }

    private static void resolve(Path path, int depth, Set<Path> out, List<String> unscannable) {
        if (path == null || depth > MAX_DEPTH) {
            return;
        }
        if (isDefaultFileSystem(path)) {
            if (exists(path)) {
                out.add(path);
            }
            return;
        }

        String scheme = scheme(path);
        if ("union".equals(scheme)) {
            List<Path> bases = unionBasePaths(path);
            if (bases != null && !bases.isEmpty()) {
                for (Path base : bases) {
                    resolve(base, depth + 1, out, unscannable);
                }
                return;
            }
        }
        if ("jar".equals(scheme)) {
            Path backing = zipBackingFile(path);
            if (backing != null) {
                resolve(backing, depth + 1, out, unscannable);
                return;
            }
        }

        Path extracted = extractNested(path);
        if (extracted != null) {
            out.add(extracted);
            return;
        }
        unscannable.add(String.valueOf(path.toUri()));
    }

    public static boolean isDefaultFileSystem(Path path) {
        try {
            return path.getFileSystem() == java.nio.file.FileSystems.getDefault();
        } catch (Throwable th) {
            return false;
        }
    }

    private static String scheme(Path path) {
        try {
            return path.getFileSystem().provider().getScheme();
        } catch (Throwable th) {
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    static List<Path> unionBasePaths(Path unionPath) {
        FileSystem fs = unionPath.getFileSystem();
        try {
            Method method = fs.getClass().getDeclaredMethod("getBasePaths");
            method.setAccessible(true);
            Object result = method.invoke(fs);
            if (result instanceof List) {
                return (List<Path>) result;
            }
        } catch (Throwable th) {
            Log.debug("UnionFileSystem.getBasePaths() is not reachable ({}), falling back to getPrimaryPath()", th);
        }
        try {
            Method method = fs.getClass().getMethod("getPrimaryPath");
            Object result = method.invoke(fs);
            if (result instanceof Path) {
                return List.of((Path) result);
            }
        } catch (Throwable ignored) {
            return null;
        }
        return null;
    }

    static Path zipBackingFile(Path zipPath) {
        try {
            if (zipPath.getNameCount() != 0) {
                return null;
            }
            String raw = zipPath.toUri().toString();
            int bang = raw.lastIndexOf("!/");
            if (!raw.startsWith("jar:") || bang < 0) {
                return null;
            }
            URI inner = URI.create(raw.substring("jar:".length(), bang));
            return Paths.get(inner);
        } catch (Throwable th) {
            return null;
        }
    }

    private static Path extractNested(Path path) {
        if (Boolean.getBoolean(PROP_NO_EXTRACT)) {
            return null;
        }
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > MAX_EXTRACT_BYTES) {
                return null;
            }
            Path temp = Files.createTempFile("polymixin-nested-", ".jar");
            temp.toFile().deleteOnExit();
            Files.copy(path, temp, StandardCopyOption.REPLACE_EXISTING);
            Log.debug("extracted nested classpath root {} to {}", path.toUri(), temp);
            return temp;
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    private static boolean exists(Path path) {
        try {
            return Files.exists(path);
        } catch (Throwable th) {
            return false;
        }
    }
}
