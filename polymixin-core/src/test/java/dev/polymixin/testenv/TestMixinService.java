package dev.polymixin.testenv;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.logging.LoggerAdapterConsole;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

public final class TestMixinService extends MixinServiceAbstract implements IClassBytecodeProvider {

    private static TestMixinService instance;

    private final TestClassProvider classProvider = new TestClassProvider();
    private final TestClassTracker classTracker = new TestClassTracker();
    private final ContainerHandleVirtual container = new ContainerHandleVirtual("polymixin-test");

    public TestMixinService() {
        instance = this;
    }

    public static TestMixinService instance() {
        return instance;
    }

    public TestClassTracker tracker() {
        return this.classTracker;
    }

    public static void bootstrapMixinExtras() {
        try {
            Class<?> bootstrap = Class.forName("com.llamalad7.mixinextras.MixinExtrasBootstrap");
            bootstrap.getMethod("init").invoke(null);
        } catch (Throwable th) {
            throw new IllegalStateException("MixinExtras bootstrap failed", th);
        }
    }

    public IMixinTransformerFactory transformerFactory() {
        return this.getInternal(IMixinTransformerFactory.class);
    }

    @Override
    protected ILogger createLogger(String name) {
        return new LoggerAdapterConsole(name);
    }

    @Override
    public String getName() {
        return "PolyMixin Test Harness";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Phase getInitialPhase() {
        return Phase.DEFAULT;
    }

    @Override
    public IClassProvider getClassProvider() {
        return this.classProvider;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return null;
    }

    @Override
    public IClassTracker getClassTracker() {
        return this.classTracker;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return Collections.singletonList("dev.polymixin.testenv.TestPlatformAgent");
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return this.container;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return TestMixinService.class.getClassLoader().getResourceAsStream(name);
    }

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return this.getClassNode(name, true);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        byte[] bytes = readBytes(name);
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    public static byte[] readBytes(String name) throws ClassNotFoundException, IOException {
        String resource = name.replace('.', '/') + ".class";
        try (InputStream in = TestMixinService.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new ClassNotFoundException(name);
            }
            return in.readAllBytes();
        }
    }

    public static InputStream bytesAsStream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }
}
