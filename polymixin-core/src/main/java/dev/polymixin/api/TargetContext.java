package dev.polymixin.api;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class TargetContext {

    private final String configName;
    private final String mixinClassName;
    private final List<String> declaredTargets;
    private final ScanResult scanResult;
    private final TargetInspector inspector;

    public TargetContext(String configName, String mixinClassName, List<String> declaredTargets,
                         ScanResult scanResult, TargetInspector inspector) {
        this.configName = configName;
        this.mixinClassName = mixinClassName;
        this.declaredTargets = Collections.unmodifiableList(declaredTargets);
        this.scanResult = scanResult;
        this.inspector = inspector;
    }

    public String configName() {
        return this.configName;
    }

    public String mixinClassName() {
        return this.mixinClassName;
    }

    public List<String> declaredTargets() {
        return this.declaredTargets;
    }

    public ScanResult scanResult() {
        return this.scanResult;
    }

    /**
     * Every type below a declared target: subclasses when the target is a class, implementers and
     * extending interfaces when it is an interface.
     */
    public Set<ClassInfo> subclassesOfDeclaredTargets() {
        Set<ClassInfo> out = new LinkedHashSet<>(this.subclassesOfDeclaredClasses());
        out.addAll(this.implementersOfDeclaredTargets());
        return out;
    }

    /**
     * The types below the declared targets that are interfaces: the classes implementing them and
     * the interfaces extending them. Declared targets that are classes contribute nothing.
     */
    public Set<ClassInfo> implementersOfDeclaredTargets() {
        Set<ClassInfo> out = new LinkedHashSet<>();
        for (ClassInfo base : this.declaredTargetInfos()) {
            if (base.isInterface()) {
                addAll(out, base.getSubinterfaces());
                addAll(out, base.getClassesImplementing());
            }
        }
        return out;
    }

    private Set<ClassInfo> subclassesOfDeclaredClasses() {
        Set<ClassInfo> out = new LinkedHashSet<>();
        for (ClassInfo base : this.declaredTargetInfos()) {
            if (!base.isInterface()) {
                addAll(out, base.getSubclasses());
            }
        }
        return out;
    }

    private List<ClassInfo> declaredTargetInfos() {
        List<ClassInfo> out = new ArrayList<>();
        for (String declared : this.declaredTargets) {
            ClassInfo base = this.scanResult.getClassInfo(declared);
            if (base != null) {
                out.add(base);
            }
        }
        return out;
    }

    private static void addAll(Set<ClassInfo> out, ClassInfoList list) {
        for (ClassInfo info : list) {
            out.add(info);
        }
    }

    /**
     * Whether {@code target} declares a method with this name itself, rather than inheriting it.
     * Uses Mixin's own class metadata, so it costs no extra scanning.
     */
    public boolean declaresMethod(ClassInfo target, String methodName) {
        return this.inspector.declaresMethod(target.getName(), this.inspector.resolveMethodName(methodName));
    }

    /**
     * Whether {@code target}'s own {@code methodName} calls {@code super.methodName(...)}. When it
     * does, the patched parent implementation already runs and the subclass needs no mixin.
     */
    public boolean delegatesToSuper(ClassInfo target, String methodName) {
        return this.inspector.delegatesToSuper(target.getName(), this.inspector.resolveMethodName(methodName));
    }

    /**
     * Subclasses of the declared targets that override at least one of {@code methodNames}.
     *
     * <p>Prefer this to {@link #subclassesOfDeclaredTargets()} when the mixin exists to catch
     * subclasses that bypass a patched method: it skips subclasses that simply inherit the patched
     * implementation, which need no mixin of their own.
     *
     * <p>Pass method names exactly as you write them in an injector's {@code method =}; they are
     * mapped through the same refmap the mixin uses.
     */
    public Set<ClassInfo> subclassesOverriding(String... methodNames) {
        Set<ClassInfo> out = new LinkedHashSet<>();
        for (ClassInfo sub : this.subclassesOfDeclaredTargets()) {
            for (String name : methodNames) {
                if (this.declaresMethod(sub, name)) {
                    out.add(sub);
                    break;
                }
            }
        }
        return out;
    }

    /**
     * Subclasses that override one of {@code methodNames} <em>without</em> calling {@code super},
     * so the patched parent implementation never runs for them.
     *
     * <p>This is the set the motivating case actually wants. Compared with
     * {@link #subclassesOverriding(String...)} it also drops subclasses that override and delegate,
     * which are already covered through their {@code super} call and would otherwise see a
     * non-cancelling injector fire twice.
     *
     * <p>Pass method names exactly as you write them in an injector's {@code method =}.
     */
    public Set<ClassInfo> subclassesBypassing(String... methodNames) {
        Set<ClassInfo> out = new LinkedHashSet<>();
        for (ClassInfo sub : this.subclassesOfDeclaredTargets()) {
            for (String name : methodNames) {
                if (this.declaresMethod(sub, name) && !this.delegatesToSuper(sub, name)) {
                    out.add(sub);
                    break;
                }
            }
        }
        return out;
    }

    /**
     * Subclasses that can actually bypass a patch on a declared target: they declare at least one
     * override that does not call {@code super}.
     *
     * <p>This is the default for {@link DynamicTargets} and needs no method names. Subclasses that
     * declare nothing relevant cannot match an injector anyway, and subclasses whose overrides all
     * delegate already run the patched parent, so patching them only risks firing an injector twice.
     *
     * <p>Falls back to every subclass when the mixin injects into a constructor, where the
     * reasoning does not hold.
     */
    public Set<ClassInfo> subclassesThatBypass() {
        if (this.inspector.mixinTargetsConstructor()) {
            return this.subclassesOfDeclaredTargets();
        }
        Set<String> injected = this.inspector.injectedMethodNames();
        if (injected != null && injected.isEmpty()) {
            injected = null;
        }

        Set<ClassInfo> out = new LinkedHashSet<>();
        for (ClassInfo sub : this.implementersOfDeclaredTargets()) {
            if (injected == null || this.declaresAny(sub, injected)) {
                out.add(sub);
            }
        }
        for (ClassInfo sub : this.subclassesOfDeclaredClasses()) {
            boolean keep = injected == null
                    ? this.inspector.bypassesAnyOverride(sub.getName())
                    : this.bypassesAny(sub, injected);
            if (keep) {
                out.add(sub);
            }
        }
        return out;
    }

    private boolean declaresAny(ClassInfo target, Set<String> methodNames) {
        for (String name : methodNames) {
            if (this.declaresMethod(target, name)) {
                return true;
            }
        }
        return false;
    }

    private boolean bypassesAny(ClassInfo target, Set<String> methodNames) {
        for (String name : methodNames) {
            if (this.declaresMethod(target, name) && !this.delegatesToSuper(target, name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "TargetContext[" + this.mixinClassName + " in " + this.configName + "]";
    }
}
