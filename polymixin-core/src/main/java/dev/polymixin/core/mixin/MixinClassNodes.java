package dev.polymixin.core.mixin;

import dev.polymixin.core.diagnostics.Log;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.reflect.Field;

public final class MixinClassNodes {

    public static final String PROP_NO_PEEK = "polymixin.mixin.noPeek";

    private static Field pendingState;
    private static Field state;
    private static Field stateClassNode;
    private static boolean resolved;
    private static boolean available;

    private MixinClassNodes() {
    }

    public static ClassNode peek(IMixinInfo info) {
        ClassNode direct = readWithoutCopying(info);
        return direct != null ? direct : info.getClassNode(0);
    }

    public static synchronized boolean isAvailable() {
        return resolve(null);
    }

    private static ClassNode readWithoutCopying(IMixinInfo info) {
        if (Boolean.getBoolean(PROP_NO_PEEK)) {
            return null;
        }
        synchronized (MixinClassNodes.class) {
            if (!resolve(info)) {
                return null;
            }
        }
        try {
            Object holder = pendingState.get(info);
            if (holder == null) {
                holder = state.get(info);
            }
            return holder == null ? null : (ClassNode) stateClassNode.get(holder);
        } catch (Throwable th) {
            return null;
        }
    }

    private static boolean resolve(IMixinInfo sample) {
        if (resolved) {
            return available;
        }
        if (sample == null) {
            return false;
        }
        resolved = true;
        try {
            Class<?> type = sample.getClass();
            pendingState = field(type, "pendingState");
            state = field(type, "state");
            stateClassNode = field(pendingState.getType(), "classNode");
            available = true;
        } catch (Throwable th) {
            Log.debug("cannot read mixin class nodes without copying ({}), falling back to getClassNode(0)", th);
            available = false;
        }
        return available;
    }

    private static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
