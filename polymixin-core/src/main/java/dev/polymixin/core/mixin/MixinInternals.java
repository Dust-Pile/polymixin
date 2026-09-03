package dev.polymixin.core.mixin;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.service.IMixinService;

public final class MixinInternals {

    private static final String FIELD_SERVICE = "service";
    private static final String FIELD_REFMAPPER = "refMapper";
    private static final String FIELD_PLUGIN = "plugin";
    private static final String FIELD_MIXIN_MAPPING = "mixinMapping";

    private MixinInternals() {
    }

    public static IMixinService getService(IMixinConfig config) throws ReflectiveOperationException {
        return (IMixinService) field(config.getClass(), FIELD_SERVICE).get(config);
    }

    public static void setService(IMixinConfig config, IMixinService service) throws ReflectiveOperationException {
        field(config.getClass(), FIELD_SERVICE).set(config, service);
    }

    public static IReferenceMapper getReferenceMapper(IMixinConfig config) throws ReflectiveOperationException {
        return (IReferenceMapper) field(config.getClass(), FIELD_REFMAPPER).get(config);
    }

    public static void setReferenceMapper(IMixinConfig config, IReferenceMapper mapper) throws ReflectiveOperationException {
        field(config.getClass(), FIELD_REFMAPPER).set(config, mapper);
    }

    public static Object getPluginHandle(IMixinConfig config) throws ReflectiveOperationException {
        return field(config.getClass(), FIELD_PLUGIN).get(config);
    }

    public static void setPlugin(Object pluginHandle, IMixinConfigPlugin plugin) throws ReflectiveOperationException {
        field(pluginHandle.getClass(), FIELD_PLUGIN).set(pluginHandle, plugin);
    }

    /**
     * Drops one mixin from a config's target mapping so it is never applied to {@code targetName}.
     * The mixin itself, and every other target it has, are left alone.
     */
    public static boolean detachTarget(IMixinConfig config, IMixinInfo mixin, String targetName)
            throws ReflectiveOperationException {
        Object raw = field(config.getClass(), FIELD_MIXIN_MAPPING).get(config);
        if (!(raw instanceof Map)) {
            return false;
        }
        boolean detached = false;
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
            if (!targetName.equals(String.valueOf(entry.getKey()).replace('/', '.'))) {
                continue;
            }
            if (entry.getValue() instanceof Collection && ((Collection<?>) entry.getValue()).remove(mixin)) {
                detached = true;
            }
        }
        return detached;
    }

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
