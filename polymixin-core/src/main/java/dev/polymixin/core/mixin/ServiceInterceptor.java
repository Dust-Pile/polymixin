package dev.polymixin.core.mixin;

import dev.polymixin.core.registry.GeneratedMixin;
import dev.polymixin.core.registry.GeneratedRegistry;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IMixinService;

public final class ServiceInterceptor implements InvocationHandler {

    private final IMixinService delegate;
    private final IClassBytecodeProvider bytecodeProvider;

    private ServiceInterceptor(IMixinService delegate) {
        this.delegate = delegate;
        this.bytecodeProvider = wrapBytecodeProvider(delegate.getBytecodeProvider());
    }

    public static IMixinService wrap(IMixinService delegate) {
        if (Proxy.isProxyClass(delegate.getClass())
                && Proxy.getInvocationHandler(delegate) instanceof ServiceInterceptor) {
            return delegate;
        }
        return (IMixinService) Proxy.newProxyInstance(
                ServiceInterceptor.class.getClassLoader(),
                new Class<?>[]{IMixinService.class},
                new ServiceInterceptor(delegate));
    }

    private static IClassBytecodeProvider wrapBytecodeProvider(IClassBytecodeProvider delegate) {
        return (IClassBytecodeProvider) Proxy.newProxyInstance(
                ServiceInterceptor.class.getClassLoader(),
                new Class<?>[]{IClassBytecodeProvider.class},
                new BytecodeInterceptor(delegate));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("getBytecodeProvider".equals(method.getName()) && (args == null || args.length == 0)) {
            return this.bytecodeProvider;
        }
        try {
            return method.invoke(this.delegate, args);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    private static final class BytecodeInterceptor implements InvocationHandler {

        private final IClassBytecodeProvider delegate;

        BytecodeInterceptor(IClassBytecodeProvider delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("getClassNode".equals(method.getName()) && args != null && args.length > 0 && args[0] instanceof String) {
                GeneratedMixin generated = GeneratedRegistry.byGeneratedName((String) args[0]);
                if (generated != null) {
                    int flags = args.length >= 3 && args[2] instanceof Integer ? (Integer) args[2] : 0;
                    ClassNode node = new ClassNode();
                    new ClassReader(generated.bytes()).accept(node, flags);
                    return node;
                }
            }
            try {
                return method.invoke(this.delegate, args);
            } catch (InvocationTargetException ex) {
                throw ex.getCause();
            }
        }
    }
}
