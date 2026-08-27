package dev.polymixin.testenv;

import org.spongepowered.asm.service.IPropertyKey;

public final class TestPropertyKey implements IPropertyKey {

    private final String name;

    public TestPropertyKey(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TestPropertyKey && this.name.equals(((TestPropertyKey) obj).name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return this.name;
    }
}
