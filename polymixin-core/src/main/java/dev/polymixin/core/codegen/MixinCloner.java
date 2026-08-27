package dev.polymixin.core.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

public final class MixinCloner {

    private static final String MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW_DESC = "Lorg/spongepowered/asm/mixin/Shadow;";

    private MixinCloner() {
    }

    public static CloneResult clone(ClassNode source, String generatedRef, String targetRef,
                                    boolean relaxRequirements, TargetMembers members) {
        if ((source.access & Opcodes.ACC_INTERFACE) != 0) {
            return CloneResult.skipped("interface mixin, already inherited by every subclass");
        }

        String originalRef = source.name;

        ClassNode out = new ClassNode();
        source.accept(new ClassRemapper(out, new SimpleRemapper(originalRef, generatedRef)));

        List<String> stripped = new ArrayList<>();
        String unresolvable = stripUnresolvableShadows(out, members, stripped);
        if (unresolvable != null) {
            return CloneResult.skipped(unresolvable);
        }

        retargetOrphanedInnerClasses(out, originalRef, generatedRef);
        rewriteMixinAnnotation(out, targetRef);
        if (relaxRequirements) {
            relaxInjectors(out);
        }

        ClassWriter writer = new ClassWriter(0);
        out.accept(writer);
        return CloneResult.generated(writer.toByteArray(), stripped);
    }

    public static Set<String> innerClassesOf(ClassNode source) {
        Set<String> inner = new LinkedHashSet<>();
        if (source.innerClasses == null) {
            return inner;
        }
        for (InnerClassNode node : source.innerClasses) {
            if (node.name.startsWith(source.name + "$")) {
                inner.add(node.name);
            }
        }
        return inner;
    }

    private static String stripUnresolvableShadows(ClassNode out, TargetMembers members, List<String> stripped) {
        if (members == null) {
            return null;
        }

        for (Iterator<FieldNode> iter = out.fields.iterator(); iter.hasNext();) {
            FieldNode field = iter.next();
            if (AnnotationNodes.find(field.visibleAnnotations, SHADOW_DESC) == null) {
                continue;
            }
            if (members.declaresField(field.name, field.desc)) {
                continue;
            }
            if (!members.inheritsAccessibleField(field.name, field.desc)) {
                return "@Shadow field " + field.name + " is neither declared nor accessibly inherited";
            }
            iter.remove();
            stripped.add("field " + field.name);
        }

        for (Iterator<MethodNode> iter = out.methods.iterator(); iter.hasNext();) {
            MethodNode method = iter.next();
            if (AnnotationNodes.find(method.visibleAnnotations, SHADOW_DESC) == null) {
                continue;
            }
            if (members.declaresMethod(method.name, method.desc)) {
                continue;
            }
            if (!members.inheritsAccessibleMethod(method.name, method.desc)) {
                return "@Shadow method " + method.name + " is neither declared nor accessibly inherited";
            }
            iter.remove();
            stripped.add("method " + method.name);
        }
        return null;
    }

    private static void retargetOrphanedInnerClasses(ClassNode out, String originalRef, String generatedRef) {
        if (out.innerClasses == null) {
            return;
        }
        for (InnerClassNode node : out.innerClasses) {
            if (node.outerName == null && node.name.startsWith(originalRef + "$")) {
                node.outerName = generatedRef;
            }
        }
    }

    private static void rewriteMixinAnnotation(ClassNode out, String targetRef) {
        AnnotationNode mixin = AnnotationNodes.find(out.invisibleAnnotations, MIXIN_DESC);
        if (mixin == null) {
            mixin = AnnotationNodes.find(out.visibleAnnotations, MIXIN_DESC);
        }
        if (mixin == null) {
            throw new IllegalStateException("Mixin class " + out.name + " has no @Mixin annotation");
        }
        List<Object> value = new ArrayList<>(Arrays.asList((Object) Type.getObjectType(targetRef)));
        AnnotationNodes.put(mixin, "value", value);
        AnnotationNodes.remove(mixin, "targets");
    }

    private static void relaxInjectors(ClassNode out) {
        for (MethodNode method : out.methods) {
            relax(method.visibleAnnotations);
            relax(method.invisibleAnnotations);
        }
    }

    private static void relax(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return;
        }
        for (AnnotationNode annotation : annotations) {
            if (isGroup(annotation.desc)) {
                AnnotationNodes.put(annotation, "min", 0);
                continue;
            }
            if (!AnnotationNodes.isInjector(annotation.desc) || !AnnotationNodes.hasSelector(annotation)) {
                continue;
            }
            AnnotationNodes.put(annotation, "require", 0);
            AnnotationNodes.put(annotation, "expect", 0);
        }
    }

    private static boolean isGroup(String desc) {
        return "Lorg/spongepowered/asm/mixin/injection/Group;".equals(desc);
    }
}
