package dev.polymixin.core.mixin;

import org.spongepowered.asm.mixin.extensibility.IMixinConfig;

public final class ConfigOwner {

    private static final String FABRIC_MOD_ID = "fabric-modId";

    private ConfigOwner() {
    }

    public static String modId(IMixinConfig config) {
        try {
            if (!config.hasDecoration(FABRIC_MOD_ID)) {
                return null;
            }
            Object value = config.getDecoration(FABRIC_MOD_ID);
            return value instanceof String && !((String) value).isEmpty() ? (String) value : null;
        } catch (Throwable th) {
            return null;
        }
    }
}
