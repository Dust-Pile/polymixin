package dev.polymixin.api;

import io.github.classgraph.ClassInfo;

import java.util.Collection;

public final class BypassingSubclasses implements DynamicTargetProvider {

    @Override
    public Collection<ClassInfo> provideTargets(TargetContext ctx) {
        return ctx.subclassesThatBypass();
    }
}
