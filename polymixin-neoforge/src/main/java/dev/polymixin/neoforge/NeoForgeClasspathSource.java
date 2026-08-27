package dev.polymixin.neoforge;

import dev.polymixin.core.platform.ClasspathSource;
import dev.polymixin.core.platform.DevModFolders;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;

import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class NeoForgeClasspathSource implements ClasspathSource {

    @Override
    public String platformName() {
        return "NeoForge 1.21.1";
    }

    @Override
    public boolean isAvailable() {
        try {
            return LoadingModList.get() != null;
        } catch (Throwable th) {
            return false;
        }
    }

    @Override
    public List<Path> classpathRoots() {
        List<Path> roots = new ArrayList<>();
        LoadingModList modList = LoadingModList.get();
        if (modList == null) {
            return roots;
        }
        for (ModFileInfo info : modList.getModFiles()) {
            try {
                roots.add(info.getFile().getSecureJar().getRootPath());
            } catch (Throwable ignored) {
                continue;
            }
        }
        roots.addAll(DevModFolders.paths());
        return roots;
    }

    @Override
    public Set<String> dependentModIds(String modId) {
        LoadingModList modList = LoadingModList.get();
        if (modList == null) {
            return null;
        }
        Set<String> dependents = new LinkedHashSet<>();
        for (IModInfo mod : modList.getMods()) {
            for (IModInfo.ModVersion dependency : mod.getDependencies()) {
                if (modId.equals(dependency.getModId())) {
                    dependents.add(mod.getModId());
                    break;
                }
            }
        }
        return dependents;
    }
}
