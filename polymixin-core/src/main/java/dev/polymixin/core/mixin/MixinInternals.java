package dev.polymixin.core.mixin;

import java.lang.reflect.Field;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.service.IMixinService;

public final class MixinInternals {

    private static final String FIELD_SERVICE = "service";
    private static final String FIELD_REFMAPPER = "refMapper";
    private static final String FIELD_PLUGIN = "plugin";

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

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
