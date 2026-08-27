package dev.polymixin.core.platform;

import dev.polymixin.core.diagnostics.Log;

import java.util.Locale;
import java.util.Set;

public final class Dependents {

    public static final String MOD_ID = "polymixin";
    public static final String PROP_DISCOVERY = "polymixin.discovery";

    private static Set<String> modIds;
    private static Mode mode;

    private Dependents() {
    }

    public enum Mode {
        ALL,
        DEPENDENTS,
        PLUGINS_ONLY
    }

    public static synchronized Mode mode() {
        if (mode != null) {
            return mode;
        }
        String requested = System.getProperty(PROP_DISCOVERY, "auto").trim().toLowerCase(Locale.ROOT);
        if ("plugins".equals(requested)) {
            modIds = Set.of();
            return mode = Mode.PLUGINS_ONLY;
        }
        if ("dependents".equals(requested)) {
            return mode = resolveDependents();
        }
        if (!"auto".equals(requested) && !"all".equals(requested)) {
            Log.warn("unrecognised -D{}={}, falling back to auto", PROP_DISCOVERY, requested);
        }
        modIds = null;
        return mode = Mode.ALL;
    }

    private static Mode resolveDependents() {
        ClasspathSource source = Platform.source();
        Set<String> dependents = source == null ? null : query(source);
        if (dependents == null) {
            modIds = null;
            Log.warn("{}=dependents was requested but this platform cannot enumerate dependencies,"
                    + " inspecting every mixin instead", PROP_DISCOVERY);
            return Mode.ALL;
        }
        modIds = dependents;
        if (dependents.isEmpty()) {
            Log.info("{}=dependents was requested and no loaded mod declares a dependency on '{}',"
                    + " so @DynamicTargets discovery is disabled", PROP_DISCOVERY, MOD_ID);
            return Mode.PLUGINS_ONLY;
        }
        Log.debug("restricting @DynamicTargets discovery to {} dependent mod(s): {}", dependents.size(), dependents);
        return Mode.DEPENDENTS;
    }

    public static synchronized boolean mayDeclareAnnotation(String owningModId) {
        Mode current = mode();
        if (current == Mode.PLUGINS_ONLY) {
            return false;
        }
        if (current == Mode.ALL || owningModId == null || modIds == null) {
            return true;
        }
        return modIds.contains(owningModId);
    }

    private static Set<String> query(ClasspathSource source) {
        try {
            return source.dependentModIds(MOD_ID);
        } catch (Throwable th) {
            Log.warn("{} could not enumerate dependencies ({}), inspecting every mixin",
                    source.platformName(), th);
            return null;
        }
    }

    public static synchronized void reset() {
        mode = null;
        modIds = null;
    }
}
