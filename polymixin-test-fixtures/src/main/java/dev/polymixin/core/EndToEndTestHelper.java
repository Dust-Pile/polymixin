package dev.polymixin.core;

import dev.polymixin.core.platform.ClasspathSource;
import dev.polymixin.core.platform.Platform;
import dev.polymixin.testenv.TestMixinService;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class EndToEndTestHelper {

    private final Map<String, Class<?>> loaded = new HashMap<>();

    private IMixinTransformer transformer;
    private GameClassLoader loader;

    private final TestClassesRootGetter testClassesRoot;
    private final Runnable externalBootstrapper;

    public EndToEndTestHelper(Supplier<Class<?>> classpathHint, Runnable externalBootstrapper) {
        testClassesRoot = () -> Paths.get(classpathHint.get().getProtectionDomain().getCodeSource().getLocation().toURI());
        this.externalBootstrapper = externalBootstrapper;
    }

    public ClassLoader getLoader() {
        return loader;
    }

    private static final class GameClassLoader extends ClassLoader {

        GameClassLoader() {
            super(EndToEndTestHelper.class.getClassLoader());
        }

        Class<?> define(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    private interface TestClassesRootGetter {
        Path get() throws Exception;
    }

    private Path testClassesRoot() throws Exception {
        return testClassesRoot.get();
    }

    private synchronized void bootstrap() throws Exception {
        if (transformer != null) {
            return;
        }
        Path root = testClassesRoot();
        Platform.override(new ClasspathSource() {
            @Override
            public String platformName() {
                return "unit-test";
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public List<Path> classpathRoots() {
                return List.of(root);
            }
        });

        MixinBootstrap.init();
        transformer = TestMixinService.instance().transformerFactory().createTransformer();
        TestMixinService.bootstrapMixinExtras();
        externalBootstrapper.run();
        loader = new GameClassLoader();
    }

    public Class<?> transformAndLoad(String name) throws Exception {
        bootstrap();
        Class<?> cached = loaded.get(name);
        if (cached != null) {
            return cached;
        }
        byte[] original = TestMixinService.readBytes(name);
        byte[] transformed = transformer.transformClassBytes(name, name, original);
        Class<?> defined = loader.define(name, transformed);
        loaded.put(name, defined);
        return defined;
    }

    public Object invoke(Class<?> type, String method) throws Exception {
        Object instance = type.getDeclaredConstructor().newInstance();
        Method target = type.getMethod(method);
        return target.invoke(instance);
    }
}
