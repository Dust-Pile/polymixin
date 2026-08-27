package dev.polymixin.api;

import io.github.classgraph.ClassInfo;

import java.util.Collection;

public final class SubclassesOfDeclaredTargets implements DynamicTargetProvider {

    @Override
    public Collection<ClassInfo> provideTargets(TargetContext ctx) {
        return ctx.subclassesOfDeclaredTargets();
    }
}
