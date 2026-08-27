package dev.polymixin.api;

import io.github.classgraph.ClassInfo;

import java.util.Collection;

public interface DynamicTargetProvider {

    Collection<ClassInfo> provideTargets(TargetContext ctx);

    default boolean relaxInjectionRequirements() {
        return true;
    }
}
