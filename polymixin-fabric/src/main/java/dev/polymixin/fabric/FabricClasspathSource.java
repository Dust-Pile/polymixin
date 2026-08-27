package dev.polymixin.fabric;

import dev.polymixin.core.platform.ClasspathSource;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModDependency;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class FabricClasspathSource implements ClasspathSource {

    private static final String JAVA_MOD_ID = "java";

    @Override
    public String platformName() {
        return "Fabric 1.21.1";
    }

    @Override
    public boolean isAvailable() {
        try {
            return FabricLoader.getInstance() != null;
        } catch (Throwable th) {
            return false;
        }
    }

    @Override
    public List<Path> classpathRoots() {
        FabricLoader loader = FabricLoader.getInstance();
        List<Path> roots = new ArrayList<>();

        for (ModContainer container : loader.getAllMods()) {
            if (JAVA_MOD_ID.equals(container.getMetadata().getId())) {
                continue;
            }
            try {
                roots.addAll(container.getRootPaths());
            } catch (Throwable ignored) {
                continue;
            }
        }

        if (loader.isDevelopmentEnvironment()) {
            roots.addAll(classPathEntries());
        }
        return roots;
    }

    private static List<Path> classPathEntries() {
        List<Path> entries = new ArrayList<>();
        String raw = System.getProperty("java.class.path");
        if (raw == null || raw.isEmpty()) {
            return entries;
        }
        for (String element : raw.split(File.pathSeparator)) {
            if (element.isEmpty()) {
                continue;
            }
            try {
                Path path = Paths.get(element);
                if (Files.isDirectory(path)) {
                    entries.add(path);
                }
            } catch (Throwable ignored) {
                continue;
            }
        }
        return entries;
    }

    @Override
    public Set<String> dependentModIds(String modId) {
        Set<String> dependents = new LinkedHashSet<>();
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            for (ModDependency dependency : container.getMetadata().getDependencies()) {
                if (dependency.getKind().isPositive() && modId.equals(dependency.getModId())) {
                    dependents.add(container.getMetadata().getId());
                    break;
                }
            }
        }
        return dependents;
    }
}
