package dev.polymixin.core.scan;

import dev.polymixin.core.diagnostics.Log;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.Resource;
import io.github.classgraph.ScanResult;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.function.BiPredicate;
import java.util.Map;

public final class SuperDelegation {

    private static final Map<String, ClassNode> CACHE = new HashMap<>();

    private SuperDelegation() {
    }

    /**
     * Whether {@code className} declares any override that does <em>not</em> delegate to its super
     * implementation. Those are the only subclasses that can bypass a patch on the parent: a class
     * that declares nothing relevant cannot match an injector at all, and one whose overrides all
     * call {@code super} already runs the patched parent.
     */
    public static synchronized boolean bypassesAnyOverride(ScanResult scan, String className,
                                                           BiPredicate<String, String> isOverride) {
        ClassNode node = read(scan, className);
        if (node == null) {
            return true;
        }
        String self = className.replace('.', '/');
        for (MethodNode method : node.methods) {
            if (!isCandidate(method) || !isOverride.test(method.name, method.desc)) {
                continue;
            }
            if (!callsSuper(method, self)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCandidate(MethodNode method) {
        int skip = Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_ABSTRACT;
        return (method.access & skip) == 0
                && !method.name.equals("<init>")
                && !method.name.equals("<clinit>");
    }

    /**
     * Whether {@code method} reaches {@code super.name(...)} on every path, judged by there being no
     * branch instruction before the call.
     *
     * <p>Deliberately conservative. {@code return super.foo(a) || b} qualifies, because the call
     * runs before anything can jump past it. {@code return b || super.foo(a)} does not, because the
     * short-circuit skips the parent, and skipping such a subclass would silently drop the patch on
     * that path. When in doubt this answers "no", which keeps the subclass.
     */
    private static boolean callsSuper(MethodNode method, String self) {
        if (method.instructions == null) {
            return false;
        }
        for (AbstractInsnNode insn : method.instructions) {
            if (isBranch(insn.getOpcode())) {
                return false;
            }
            if (insn.getOpcode() != Opcodes.INVOKESPECIAL || !(insn instanceof MethodInsnNode)) {
                continue;
            }
            MethodInsnNode call = (MethodInsnNode) insn;
            if (call.name.equals(method.name) && call.desc.equals(method.desc) && !call.owner.equals(self)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBranch(int opcode) {
        return (opcode >= Opcodes.IFEQ && opcode <= Opcodes.JSR)
                || opcode == Opcodes.TABLESWITCH
                || opcode == Opcodes.LOOKUPSWITCH
                || opcode == Opcodes.IFNULL
                || opcode == Opcodes.IFNONNULL;
    }

    public static synchronized boolean delegatesToSuper(ScanResult scan, String className, String methodName) {
        ClassNode node = read(scan, className);
        if (node == null) {
            return false;
        }
        String self = className.replace('.', '/');
        for (MethodNode method : node.methods) {
            if (method.name.equals(methodName) && isCandidate(method) && callsSuper(method, self)) {
                return true;
            }
        }
        return false;
    }

    private static ClassNode read(ScanResult scan, String className) {
        if (CACHE.containsKey(className)) {
            return CACHE.get(className);
        }
        ClassNode node = null;
        try {
            ClassInfo info = scan.getClassInfo(className);
            Resource resource = info == null ? null : info.getResource();
            if (resource != null) {
                node = new ClassNode();
                new ClassReader(resource.load()).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
        } catch (Throwable th) {
            Log.debug("cannot read {} to check super delegation: {}", className, th);
            node = null;
        }
        CACHE.put(className, node);
        return node;
    }

    public static synchronized void clear() {
        CACHE.clear();
    }
}
