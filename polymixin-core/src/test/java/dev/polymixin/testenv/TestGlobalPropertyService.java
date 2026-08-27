package dev.polymixin.testenv;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.HashMap;
import java.util.Map;

public final class TestGlobalPropertyService implements IGlobalPropertyService {

    private final Map<IPropertyKey, Object> properties = new HashMap<>();

    @Override
    public IPropertyKey resolveKey(String name) {
        return new TestPropertyKey(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(IPropertyKey key) {
        return (T) this.properties.get(key);
    }

    @Override
    public void setProperty(IPropertyKey key, Object value) {
        this.properties.put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(IPropertyKey key, T defaultValue) {
        Object value = this.properties.get(key);
        return value != null ? (T) value : defaultValue;
    }

    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue) {
        Object value = this.properties.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
