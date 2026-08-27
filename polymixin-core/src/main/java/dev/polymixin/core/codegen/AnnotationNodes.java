package dev.polymixin.core.codegen;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.tree.AnnotationNode;

public final class AnnotationNodes {

    private AnnotationNodes() {
    }

    public static AnnotationNode find(List<AnnotationNode> nodes, String desc) {
        if (nodes == null) {
            return null;
        }
        for (AnnotationNode node : nodes) {
            if (desc.equals(node.desc)) {
                return node;
            }
        }
        return null;
    }

    public static Object get(AnnotationNode node, String key) {
        if (node.values == null) {
            return null;
        }
        for (int i = 0; i < node.values.size() - 1; i += 2) {
            if (key.equals(node.values.get(i))) {
                return node.values.get(i + 1);
            }
        }
        return null;
    }

    public static void put(AnnotationNode node, String key, Object value) {
        if (node.values == null) {
            node.values = new ArrayList<>();
        }
        for (int i = 0; i < node.values.size() - 1; i += 2) {
            if (key.equals(node.values.get(i))) {
                node.values.set(i + 1, value);
                return;
            }
        }
        node.values.add(key);
        node.values.add(value);
    }

    public static void remove(AnnotationNode node, String key) {
        if (node.values == null) {
            return;
        }
        for (int i = 0; i < node.values.size() - 1; i += 2) {
            if (key.equals(node.values.get(i))) {
                node.values.remove(i + 1);
                node.values.remove(i);
                return;
            }
        }
    }

    public static boolean isInjector(String desc) {
        return desc.startsWith("Lorg/spongepowered/asm/mixin/injection/")
                || desc.startsWith("Lcom/llamalad7/mixinextras/injector/");
    }

    public static boolean hasSelector(AnnotationNode node) {
        return get(node, "method") != null || get(node, "target") != null;
    }
}
