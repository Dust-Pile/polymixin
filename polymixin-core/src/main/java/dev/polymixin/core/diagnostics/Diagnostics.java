package dev.polymixin.core.diagnostics;

import dev.polymixin.core.PolyMixin;
import dev.polymixin.core.mixin.MixinRegistryAccess;
import dev.polymixin.core.mixin.MixinClassNodes;
import dev.polymixin.core.scan.ClassGraphs;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

public final class Diagnostics {

    public static final String PROP_STRICT = "polymixin.strict";

    private Diagnostics() {
    }

    public static void checkGlobalHooks() {
        Map<String, String> failures = new LinkedHashMap<>();

        if (!MixinRegistryAccess.isAvailable()) {
            failures.put("ClassInfo.getMixins()",
                    "cannot enumerate the mixins registered against a target class; no provider will ever be found");
        }
        if (!MixinClassNodes.isAvailable()) {
            Log.debug("MixinInfo.State.classNode is not readable; @DynamicTargets discovery will copy"
                    + " each mixin's class node instead, which is slower but correct");
        }
        if (!ClassGraphs.canPreserveFileSystems()) {
            failures.put("ClassGraph.scanSpec",
                    "classpath roots will be passed as strings, which loses zip and union filesystems");
        }
        report("global", failures);
    }

    public static void checkConfigHooks(IMixinConfig config) {
        Map<String, String> failures = new LinkedHashMap<>();
        for (String field : new String[]{"service", "refMapper", "plugin"}) {
            try {
                config.getClass().getDeclaredField(field).setAccessible(true);
            } catch (Throwable th) {
                failures.put("MixinConfig." + field, String.valueOf(th));
            }
        }
        report("config " + config.getName(), failures);
    }

    private static void report(String scope, Map<String, String> failures) {
        if (failures.isEmpty()) {
            Log.debug("hook check for {}: all hooks bound", scope);
            return;
        }
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, String> entry : failures.entrySet()) {
            Log.error("hook {} did NOT bind: {}", entry.getKey(), entry.getValue());
            lines.add(entry.getKey());
        }
        if (Boolean.getBoolean(PROP_STRICT)) {
            throw new IllegalStateException("PolyMixin hooks failed to bind for " + scope + ": " + lines
                    + " (running with -D" + PROP_STRICT + "=true)");
        }
        Log.error("PolyMixin is degraded for {}: {} hook(s) did not bind. Run with -D{}=true to fail fast instead.",
                scope, lines.size(), PROP_STRICT);
    }

}
