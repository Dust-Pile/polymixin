package dev.polymixin.core.mixin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

public final class MixinRegistryAccess {

    private static Method getMixins;
    private static Field mixinsField;
    private static boolean resolved;

    private MixinRegistryAccess() {
    }

    public static boolean isAvailable() {
        ClassInfo probe = ClassInfo.forName("java/lang/Object");
        return probe != null && read(probe) != null;
    }

    public static Set<IMixinInfo> mixinsTargeting(String className) {
        ClassInfo info = ClassInfo.fromCache(className);
        if (info == null) {
            return Set.of();
        }
        Object raw = read(info);
        if (!(raw instanceof Iterable)) {
            return Set.of();
        }
        Set<IMixinInfo> out = new LinkedHashSet<>();
        for (Object element : (Iterable<?>) raw) {
            if (element instanceof IMixinInfo) {
                out.add((IMixinInfo) element);
            }
        }
        return out;
    }

    private static synchronized Object read(ClassInfo info) {
        if (!resolved) {
            resolved = true;
            try {
                getMixins = ClassInfo.class.getDeclaredMethod("getMixins");
                getMixins.setAccessible(true);
            } catch (Throwable th) {
                getMixins = null;
            }
            try {
                mixinsField = ClassInfo.class.getDeclaredField("mixins");
                mixinsField.setAccessible(true);
            } catch (Throwable th) {
                mixinsField = null;
            }
        }
        if (getMixins != null) {
            try {
                return getMixins.invoke(info);
            } catch (Throwable ignored) {
                getMixins = null;
            }
        }
        if (mixinsField != null) {
            try {
                return mixinsField.get(info);
            } catch (Throwable ignored) {
                mixinsField = null;
            }
        }
        return null;
    }
}
