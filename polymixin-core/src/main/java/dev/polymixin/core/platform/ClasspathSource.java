package dev.polymixin.core.platform;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public interface ClasspathSource {

    String platformName();

    boolean isAvailable();

    List<Path> classpathRoots();

    /**
     * Mod ids that declare a dependency on {@code modId}, or {@code null} if this platform cannot
     * answer. Returning {@code null} disables dependency gating; returning an empty set asserts
     * that nothing depends on the mod.
     */
    default Set<String> dependentModIds(String modId) {
        return null;
    }
}
