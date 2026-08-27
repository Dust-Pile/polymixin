package dev.polymixin.core.mixin;

import dev.polymixin.api.DynamicTargetProvider;
import dev.polymixin.api.DynamicTargets;
import dev.polymixin.api.BypassingSubclasses;
import dev.polymixin.core.codegen.AnnotationNodes;
import dev.polymixin.core.diagnostics.Log;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DynamicTargetsAnnotation {

    private static final String DESC = "L" + DynamicTargets.class.getName().replace('.', '/') + ";";

    private static final Map<String, DynamicTargetProvider> PROVIDERS = new LinkedHashMap<>();

    private DynamicTargetsAnnotation() {
    }

    public static AnnotationNode find(IMixinInfo info) {
        ClassNode node;
        try {
            node = MixinClassNodes.peek(info);
        } catch (Throwable th) {
            return null;
        }
        if (node == null) {
            return null;
        }
        AnnotationNode found = AnnotationNodes.find(node.invisibleAnnotations, DESC);
        return found != null ? found : AnnotationNodes.find(node.visibleAnnotations, DESC);
    }

    public static boolean relaxInjectionRequirements(AnnotationNode annotation) {
        Object value = AnnotationNodes.get(annotation, "relaxInjectionRequirements");
        return !(value instanceof Boolean) || (Boolean) value;
    }

    public static synchronized DynamicTargetProvider provider(AnnotationNode annotation, String mixinClassName) {
        Object value = AnnotationNodes.get(annotation, "value");
        String className = value instanceof Type
                ? ((Type) value).getClassName()
                : BypassingSubclasses.class.getName();

        DynamicTargetProvider cached = PROVIDERS.get(className);
        if (cached != null) {
            return cached;
        }
        try {
            Class<?> type = MixinService.getService().getClassProvider().findClass(className, true);
            DynamicTargetProvider provider = (DynamicTargetProvider) type.getDeclaredConstructor().newInstance();
            PROVIDERS.put(className, provider);
            return provider;
        } catch (Throwable th) {
            Log.error("@DynamicTargets on {} names provider {} which could not be instantiated;"
                    + " it needs a public no-argument constructor and must live outside the mixin package: {}",
                    mixinClassName, className, th);
            return null;
        }
    }
}
