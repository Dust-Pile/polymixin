package dev.polymixin.core.mixin;

import dev.polymixin.core.registry.GeneratedMixin;
import dev.polymixin.core.registry.GeneratedRegistry;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;

public final class AliasingReferenceMapper implements IReferenceMapper {

    private final IReferenceMapper delegate;

    private AliasingReferenceMapper(IReferenceMapper delegate) {
        this.delegate = delegate;
    }

    public static IReferenceMapper wrap(IReferenceMapper delegate) {
        if (delegate instanceof AliasingReferenceMapper) {
            return delegate;
        }
        return new AliasingReferenceMapper(delegate);
    }

    static String retargetOwner(String reference, String remapped, GeneratedMixin generated) {
        if (remapped == null || remapped.length() < 2 || remapped.charAt(0) != 'L') {
            return remapped;
        }
        if (reference != null && !reference.isEmpty() && reference.charAt(0) == 'L') {
            return remapped;
        }
        int end = remapped.indexOf(';');
        if (end < 2) {
            return remapped;
        }
        String owner = remapped.substring(1, end);
        if (!generated.declaredTargetRefs().contains(owner)) {
            return remapped;
        }
        return "L" + generated.targetRef() + remapped.substring(end);
    }

    private static String originalRef(String className, GeneratedMixin generated) {
        return className.indexOf('/') >= 0
                ? generated.originalName().replace('.', '/')
                : generated.originalName();
    }

    @Override
    public boolean isDefault() {
        return this.delegate.isDefault();
    }

    @Override
    public String getResourceName() {
        return this.delegate.getResourceName();
    }

    @Override
    public String getStatus() {
        return this.delegate.getStatus();
    }

    @Override
    public String getContext() {
        return this.delegate.getContext();
    }

    @Override
    public void setContext(String context) {
        this.delegate.setContext(context);
    }

    @Override
    public String remap(String className, String reference) {
        GeneratedMixin generated = className == null ? null : GeneratedRegistry.byGeneratedName(className);
        if (generated == null) {
            return this.delegate.remap(className, reference);
        }
        return retargetOwner(reference, this.delegate.remap(originalRef(className, generated), reference), generated);
    }

    @Override
    public String remapWithContext(String context, String className, String reference) {
        GeneratedMixin generated = className == null ? null : GeneratedRegistry.byGeneratedName(className);
        if (generated == null) {
            return this.delegate.remapWithContext(context, className, reference);
        }
        return retargetOwner(reference,
                this.delegate.remapWithContext(context, originalRef(className, generated), reference), generated);
    }
}
