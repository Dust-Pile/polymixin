package dev.polymixin.core.mixin;

import dev.polymixin.core.codegen.TargetMembers;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

public final class MixinClassInfoMembers implements TargetMembers {

    private static final int NON_PRIVATE = ClassInfo.INCLUDE_ALL & ~ClassInfo.INCLUDE_PRIVATE;

    private final ClassInfo target;

    private MixinClassInfoMembers(ClassInfo target) {
        this.target = target;
    }

    public static TargetMembers of(String targetRef) {
        ClassInfo info = ClassInfo.forName(targetRef);
        return info == null ? null : new MixinClassInfoMembers(info);
    }

    @Override
    public boolean declaresMethodNamed(String name) {
        for (ClassInfo.Method method : this.target.getMethods()) {
            if (method.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean declaresMethod(String name, String desc) {
        return this.target.findMethod(name, desc, ClassInfo.INCLUDE_ALL) != null;
    }

    @Override
    public boolean declaresField(String name, String desc) {
        return this.target.findField(name, desc, ClassInfo.INCLUDE_ALL) != null;
    }

    @Override
    public boolean inheritsAccessibleMethod(String name, String desc) {
        return this.target.findMethodInHierarchy(name, desc, ClassInfo.SearchType.SUPER_CLASSES_ONLY,
                ClassInfo.Traversal.NONE, NON_PRIVATE) != null;
    }

    @Override
    public boolean inheritsAccessibleField(String name, String desc) {
        return this.target.findFieldInHierarchy(name, desc, ClassInfo.SearchType.SUPER_CLASSES_ONLY,
                ClassInfo.Traversal.NONE, NON_PRIVATE) != null;
    }
}
