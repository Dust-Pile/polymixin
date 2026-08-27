package dev.polymixin.core.codegen;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class InjectorSelectors {

    private static final String OVERWRITE_DESC = "Lorg/spongepowered/asm/mixin/Overwrite;";

    private InjectorSelectors() {
    }

    /**
     * The raw {@code method =} selectors of every injector in a mixin, or {@code null} when any of
     * them is something this cannot read confidently (a wildcard, a quantifier, or an injector with
     * no selector at all). A {@code null} result means "assume every subclass matters".
     */
    public static Set<String> of(ClassNode mixin) {
        Set<String> selectors = new LinkedHashSet<>();
        for (MethodNode method : mixin.methods) {
            if (AnnotationNodes.find(method.visibleAnnotations, OVERWRITE_DESC) != null
                    || AnnotationNodes.find(method.invisibleAnnotations, OVERWRITE_DESC) != null) {
                selectors.add(method.name);
                continue;
            }
            if (!collect(selectors, method.visibleAnnotations) || !collect(selectors, method.invisibleAnnotations)) {
                return null;
            }
        }
        return selectors.isEmpty() ? null : selectors;
    }

    private static boolean collect(Set<String> selectors, List<AnnotationNode> annotations) {
        if (annotations == null) {
            return true;
        }
        for (AnnotationNode annotation : annotations) {
            if (!AnnotationNodes.isInjector(annotation.desc)) {
                continue;
            }
            Object value = AnnotationNodes.get(annotation, "method");
            if (value == null) {
                return AnnotationNodes.get(annotation, "target") == null;
            }
            if (value instanceof List) {
                for (Object entry : (List<?>) value) {
                    if (!add(selectors, entry)) {
                        return false;
                    }
                }
            } else if (!add(selectors, value)) {
                return false;
            }
        }
        return true;
    }

    private static boolean add(Set<String> selectors, Object raw) {
        if (!(raw instanceof String)) {
            return false;
        }
        String selector = ((String) raw).trim();
        if (selector.isEmpty() || selector.indexOf('*') >= 0
                || selector.indexOf('{') >= 0 || selector.indexOf('}') >= 0) {
            return false;
        }
        selectors.add(selector);
        return true;
    }
}
