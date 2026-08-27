package dev.polymixin.core.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GeneratedRegistry {

    private static final Map<String, GeneratedMixin> BY_GENERATED = new LinkedHashMap<>();
    private static final Map<String, List<GeneratedMixin>> BY_ORIGINAL = new LinkedHashMap<>();
    private static final Map<String, List<GeneratedMixin>> BY_CONFIG = new LinkedHashMap<>();

    private GeneratedRegistry() {
    }

    public static synchronized void register(GeneratedMixin mixin) {
        BY_GENERATED.put(mixin.generatedName(), mixin);
        BY_ORIGINAL.computeIfAbsent(mixin.originalName(), k -> new ArrayList<>()).add(mixin);
        BY_CONFIG.computeIfAbsent(mixin.configName(), k -> new ArrayList<>()).add(mixin);
    }

    public static synchronized void unregister(GeneratedMixin mixin) {
        BY_GENERATED.remove(mixin.generatedName());
        List<GeneratedMixin> byOriginal = BY_ORIGINAL.get(mixin.originalName());
        if (byOriginal != null) {
            byOriginal.remove(mixin);
            if (byOriginal.isEmpty()) {
                BY_ORIGINAL.remove(mixin.originalName());
            }
        }
        List<GeneratedMixin> byConfig = BY_CONFIG.get(mixin.configName());
        if (byConfig != null) {
            byConfig.remove(mixin);
            if (byConfig.isEmpty()) {
                BY_CONFIG.remove(mixin.configName());
            }
        }
    }

    public static synchronized GeneratedMixin byGeneratedName(String dottedOrInternal) {
        GeneratedMixin direct = BY_GENERATED.get(dottedOrInternal);
        return direct != null ? direct : BY_GENERATED.get(dottedOrInternal.replace('/', '.'));
    }

    public static synchronized List<GeneratedMixin> forConfig(String configName) {
        List<GeneratedMixin> list = BY_CONFIG.get(configName);
        return list == null ? List.of() : new ArrayList<>(list);
    }

    public static synchronized List<GeneratedMixin> forOriginal(String originalName) {
        List<GeneratedMixin> list = BY_ORIGINAL.get(originalName);
        return list == null ? List.of() : new ArrayList<>(list);
    }

    public static synchronized Collection<GeneratedMixin> all() {
        return new ArrayList<>(BY_GENERATED.values());
    }

    public static synchronized void clear() {
        BY_GENERATED.clear();
        BY_ORIGINAL.clear();
        BY_CONFIG.clear();
    }
}
