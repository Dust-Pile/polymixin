package dev.polymixin.testenv;

import org.spongepowered.asm.service.IClassTracker;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class TestClassTracker implements IClassTracker {

    private final Set<String> loaded = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> invalid = Collections.synchronizedSet(new HashSet<>());

    public void markLoaded(String name) {
        this.loaded.add(name.replace('/', '.'));
    }

    @Override
    public void registerInvalidClass(String className) {
        this.invalid.add(className.replace('/', '.'));
    }

    @Override
    public boolean isClassLoaded(String className) {
        return this.loaded.contains(className.replace('/', '.'));
    }

    @Override
    public String getClassRestrictions(String className) {
        return "";
    }
}
