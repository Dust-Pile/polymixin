package dev.polymixin.core.diagnostics;

import dev.polymixin.core.codegen.AnnotationNodes;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public final class RequireAudit {

    public static final String PROP_DISABLE = "polymixin.audit.disable";

    private RequireAudit() {
    }

    public static void audit(String mixinName, ClassNode node) {
        if (Boolean.getBoolean(PROP_DISABLE)) {
            return;
        }
        for (String finding : findings(mixinName, node)) {
            Log.warn("{}", finding);
        }
    }

    public static List<String> findings(String mixinName, ClassNode node) {
        List<String> findings = new ArrayList<>();
        for (MethodNode method : node.methods) {
            collect(findings, mixinName, method, method.visibleAnnotations);
            collect(findings, mixinName, method, method.invisibleAnnotations);
        }
        return findings;
    }

    private static void collect(List<String> findings, String mixinName, MethodNode method,
                                List<AnnotationNode> annotations) {
        if (annotations == null) {
            return;
        }
        for (AnnotationNode annotation : annotations) {
            if (!AnnotationNodes.isInjector(annotation.desc) || !AnnotationNodes.hasSelector(annotation)) {
                continue;
            }
            if (AnnotationNodes.get(annotation, "require") != null) {
                continue;
            }
            findings.add(Log.format("{}#{}: {} has no explicit require=; with dynamic targeting a failed"
                            + " inject on the declared target will not be detected. Add require = 1 (or set"
                            + " injectors.defaultRequire in the mixin config).",
                    mixinName, method.name, simpleName(annotation.desc)));
        }
    }

    private static String simpleName(String desc) {
        int slash = desc.lastIndexOf('/');
        int end = desc.length() - 1;
        return "@" + desc.substring(slash + 1, end);
    }
}
