package dev.polymixin.core.registry;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class GeneratedMixin {

    private final String configName;
    private final String originalName;
    private final String generatedName;
    private final String relativeName;
    private final String targetName;
    private final Set<String> declaredTargetRefs;
    private final byte[] bytes;

    private volatile boolean applied;
    private volatile String failure;

    public GeneratedMixin(String configName, String originalName, String generatedName, String relativeName,
                   String targetName, Collection<String> declaredTargets, byte[] bytes) {
        this.configName = configName;
        this.originalName = originalName;
        this.generatedName = generatedName;
        this.relativeName = relativeName;
        this.targetName = targetName;
        this.declaredTargetRefs = new LinkedHashSet<>();
        for (String declared : declaredTargets) {
            this.declaredTargetRefs.add(declared.replace('.', '/'));
        }
        this.bytes = bytes;
    }

    public Set<String> declaredTargetRefs() {
        return this.declaredTargetRefs;
    }

    public String targetRef() {
        return this.targetName.replace('.', '/');
    }

    public String configName() {
        return this.configName;
    }

    public String originalName() {
        return this.originalName;
    }

    public String generatedName() {
        return this.generatedName;
    }

    public String relativeName() {
        return this.relativeName;
    }

    public String targetName() {
        return this.targetName;
    }

    public byte[] bytes() {
        return this.bytes;
    }

    public boolean applied() {
        return this.applied;
    }

    public String failure() {
        return this.failure;
    }

    public void markApplied() {
        this.applied = true;
    }

    public void markFailed(String reason) {
        this.failure = reason;
    }

    @Override
    public String toString() {
        return this.generatedName + " -> " + this.targetName;
    }
}
