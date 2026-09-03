package dev.polymixin.core;

import dev.polymixin.core.platform.ClasspathSource;
import dev.polymixin.core.platform.Platform;
import dev.polymixin.core.registry.GeneratedMixin;
import dev.polymixin.core.registry.GeneratedRegistry;
import dev.polymixin.core.scan.ScanCache;
import dev.polymixin.testenv.TestMixinService;
import dev.polymixin.testgame.BaseBlock;
import dev.polymixin.testplugin.TestPlugin;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EndToEndTest {

    private static final Map<String, Class<?>> LOADED = new HashMap<>();

    private static IMixinTransformer transformer;
    private static GameClassLoader loader;

    private static final class GameClassLoader extends ClassLoader {

        GameClassLoader() {
            super(EndToEndTest.class.getClassLoader());
        }

        Class<?> define(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    private static Path testClassesRoot() throws Exception {
        return Paths.get(BaseBlock.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private static synchronized void bootstrap() throws Exception {
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

        Mixins.addConfiguration("polymixin-lib.mixins.json");
        Mixins.addConfiguration("polymixin-test.mixins.json");
        Mixins.addConfiguration("polymixin-test2.mixins.json");
        Mixins.addConfiguration("polymixin-test3.mixins.json");
        loader = new GameClassLoader();
    }

    private static Class<?> transformAndLoad(String name) throws Exception {
        bootstrap();
        Class<?> cached = LOADED.get(name);
        if (cached != null) {
            return cached;
        }
        byte[] original = TestMixinService.readBytes(name);
        byte[] transformed = transformer.transformClassBytes(name, name, original);
        Class<?> defined = loader.define(name, transformed);
        LOADED.put(name, defined);
        return defined;
    }

    private static Object invoke(Class<?> type, String method) throws Exception {
        Object instance = type.getDeclaredConstructor().newInstance();
        Method target = type.getMethod(method);
        return target.invoke(instance);
    }

    @Test
    @Order(1)
    void appliesToDeclaredTarget() throws Exception {
        Class<?> base = transformAndLoad("dev.polymixin.testgame.BaseBlock");
        assertEquals(Boolean.TRUE, invoke(base, "canSurvive"));
        assertEquals("patched", invoke(base, "describe"));
    }

    @Test
    @Order(2)
    void generatedMixinsExistForEverySubclass() throws Exception {
        transformAndLoad("dev.polymixin.testgame.BaseBlock");

        List<String> targets = new ArrayList<>();
        for (GeneratedMixin generated : GeneratedRegistry.all()) {
            targets.add(generated.originalName() + " -> " + generated.targetName());
            assertTrue(generated.generatedName().contains("_pm_"), generated.generatedName());
        }

        assertTrue(targets.contains("dev.polymixin.testmixins.MixinBaseBlock -> dev.polymixin.testgame.CropBlockA"), targets.toString());
        assertTrue(targets.contains("dev.polymixin.testmixins.MixinBaseBlock -> dev.polymixin.testgame.CropBlockB"), targets.toString());
        assertTrue(targets.contains("dev.polymixin.testmixins.MixinBaseBlock -> dev.polymixin.testgame.InheritingBlock"), targets.toString());
        assertFalse(targets.contains("dev.polymixin.testmixins.MixinBaseBlock -> dev.polymixin.testgame.UnrelatedBlock"), targets.toString());
    }

    @Test
    @Order(3)
    void appliesToOverridingSubclasses() throws Exception {
        Class<?> cropA = transformAndLoad("dev.polymixin.testgame.CropBlockA");
        assertEquals(Boolean.TRUE, invoke(cropA, "canSurvive"));
        assertEquals("patched", invoke(cropA, "describe"));

        Class<?> cropB = transformAndLoad("dev.polymixin.testgame.CropBlockB");
        assertEquals(Boolean.TRUE, invoke(cropB, "canSurvive"));
    }

    @Test
    @Order(4)
    void toleratesSubclassesWithoutTheInjectedMethod() throws Exception {
        Class<?> inheriting = transformAndLoad("dev.polymixin.testgame.InheritingBlock");
        assertEquals(7, invoke(inheriting, "unrelated"));
    }

    @Test
    @Order(5)
    void leavesUnrelatedClassesAlone() throws Exception {
        Class<?> unrelated = transformAndLoad("dev.polymixin.testgame.UnrelatedBlock");
        assertEquals(Boolean.FALSE, invoke(unrelated, "canSurvive"));
    }

    @Test
    @Order(6)
    void delegatesPluginCallbacksToTheConsumerPlugin() throws Exception {
        transformAndLoad("dev.polymixin.testgame.CropBlockA");
        assertTrue(TestPlugin.POST_APPLIED.stream().anyMatch(s -> s.contains("_pm_CropBlockA_")),
                TestPlugin.POST_APPLIED.toString());
    }

    @Test
    @Order(7)
    void resolvesShadowedMembersInheritedByTheSubclass() throws Exception {
        Class<?> base = transformAndLoad("dev.polymixin.testgame.BaseBlock");
        assertEquals(106, invoke(base, "effectiveHardness"));

        Class<?> cropA = transformAndLoad("dev.polymixin.testgame.CropBlockA");
        assertEquals(106, invoke(cropA, "effectiveHardness"));

        Class<?> cropB = transformAndLoad("dev.polymixin.testgame.CropBlockB");
        assertEquals(106, invoke(cropB, "effectiveHardness"));
    }

    @Test
    @Order(8)
    void appliesMixinExtrasInjectorsToSubclasses() throws Exception {
        Class<?> base = transformAndLoad("dev.polymixin.testgame.BaseBlock");
        assertEquals("L+extras", invoke(base, "label"));

        Class<?> cropA = transformAndLoad("dev.polymixin.testgame.CropBlockA");
        assertEquals("LA+extras", invoke(cropA, "label"));
    }

    @Test
    @Order(9)
    void appliesMultiTargetMixinsToSubclassesOfEveryDeclaredTarget() throws Exception {
        Class<?> base = transformAndLoad("dev.polymixin.testgame.BaseBlock");
        assertEquals("T[multi]", invoke(base, "tag"));

        Class<?> unrelated = transformAndLoad("dev.polymixin.testgame.UnrelatedBlock");
        assertEquals("U[multi]", invoke(unrelated, "tag"));

        Class<?> cropA = transformAndLoad("dev.polymixin.testgame.CropBlockA");
        assertEquals("TA[multi]", invoke(cropA, "tag"));

        Class<?> cropB = transformAndLoad("dev.polymixin.testgame.CropBlockB");
        assertEquals("TB[multi]", invoke(cropB, "tag"));
    }

    @Test
    @Order(14)
    void doesNotDoubleFireWhenASubclassCallsSuper() throws Exception {
        Class<?> base = transformAndLoad("dev.polymixin.testgame.BaseBlock");
        assertEquals("base!", invoke(base, "chain"));

        Class<?> cropB = transformAndLoad("dev.polymixin.testgame.CropBlockB");
        assertEquals("base!", invoke(cropB, "chain"),
                "a subclass that does not override the method inherits the patched implementation once");

        Class<?> cropA = transformAndLoad("dev.polymixin.testgame.CropBlockA");
        assertEquals("base!+A", invoke(cropA, "chain"),
                "CropBlockA reaches super.chain() unconditionally, so the default provider skips it and"
                        + " the injector fires once, in the parent");
    }

    @Test
    @Order(17)
    void theDefaultProviderSkipsSubclassesThatDelegate() throws Exception {
        transformAndLoad("dev.polymixin.testgame.BaseBlock");

        Set<String> chainTargets = new LinkedHashSet<>();
        for (GeneratedMixin generated : GeneratedRegistry.all()) {
            if (generated.originalName().endsWith("MixinSuperChain")) {
                chainTargets.add(generated.targetName());
            }
        }

        assertFalse(chainTargets.contains("dev.polymixin.testgame.CropBlockA"), chainTargets.toString());
        assertFalse(chainTargets.contains("dev.polymixin.testgame.DelegatingBlock"), chainTargets.toString());
        assertFalse(chainTargets.contains("dev.polymixin.testgame.PureDelegatingBlock"), chainTargets.toString());
        assertFalse(chainTargets.contains("dev.polymixin.testgame.InheritingBlock"),
                "declares no override, so it can never match the injector: " + chainTargets);
        assertTrue(chainTargets.contains("dev.polymixin.testgame.ConditionalSuperBlock"),
                "its super.chain() sits behind a branch, so the patch has to be copied onto it: " + chainTargets);
    }

    @Test
    @Order(18)
    void theDefaultProviderKeepsSubclassesWhoseSuperCallIsConditional() throws Exception {
        transformAndLoad("dev.polymixin.testgame.BaseBlock");
        Class<?> conditional = transformAndLoad("dev.polymixin.testgame.ConditionalSuperBlock");

        Field shortCircuit = conditional.getField("shortCircuit");
        shortCircuit.setBoolean(null, true);
        assertEquals("short!", invoke(conditional, "chain"),
                "the branch that never calls super still has to be patched");

        shortCircuit.setBoolean(null, false);
        assertEquals("base!!", invoke(conditional, "chain"),
                "the cost of keeping it is a double fire on the path that does reach super");
    }

    @Test
    @Order(16)
    void subclassesBypassingSkipsOverridersThatCallSuper() throws Exception {
        transformAndLoad("dev.polymixin.testgame.BaseBlock");

        Set<String> overriding = new LinkedHashSet<>();
        TestPlugin.LAST_CONTEXT.subclassesOverriding("chain").forEach(c -> overriding.add(c.getName()));

        Set<String> bypassing = new LinkedHashSet<>();
        TestPlugin.LAST_CONTEXT.subclassesBypassing("chain").forEach(c -> bypassing.add(c.getName()));

        assertEquals(Set.of("dev.polymixin.testgame.CropBlockA", "dev.polymixin.testgame.DelegatingBlock",
                "dev.polymixin.testgame.PureDelegatingBlock", "dev.polymixin.testgame.ConditionalSuperBlock"),
                overriding, "all four override chain()");
        assertEquals(Set.of("dev.polymixin.testgame.ConditionalSuperBlock"), bypassing,
                "only ConditionalSuperBlock can return without ever reaching super.chain()");

        Set<String> bypassingTag = new LinkedHashSet<>();
        TestPlugin.LAST_CONTEXT.subclassesBypassing("tag").forEach(c -> bypassingTag.add(c.getName()));
        assertTrue(bypassingTag.contains("dev.polymixin.testgame.DelegatingBlock"),
                "tag() is overridden without calling super, so it does bypass: " + bypassingTag);
    }

    @Test
    @Order(15)
    void subclassesOverridingFiltersToActualOverriders() throws Exception {
        transformAndLoad("dev.polymixin.testgame.BaseBlock");

        Set<String> all = new LinkedHashSet<>();
        TestPlugin.LAST_CONTEXT.subclassesOfDeclaredTargets().forEach(c -> all.add(c.getName()));

        Set<String> overriders = new LinkedHashSet<>();
        TestPlugin.LAST_CONTEXT.subclassesOverriding("chain").forEach(c -> overriders.add(c.getName()));

        assertTrue(all.contains("dev.polymixin.testgame.CropBlockB"),
                "the unfiltered set includes subclasses that never override");
        assertEquals(Set.of("dev.polymixin.testgame.CropBlockA", "dev.polymixin.testgame.DelegatingBlock",
                "dev.polymixin.testgame.PureDelegatingBlock", "dev.polymixin.testgame.ConditionalSuperBlock"),
                overriders, "CropBlockB and InheritingBlock inherit chain() rather than declaring it");
    }

    @Test
    @Order(13)
    void discoversMixinsByAnnotationWithNoConfigPlugin() throws Exception {
        Class<?> base = transformAndLoad("dev.polymixin.testgame.BaseBlock");
        assertEquals("B[annotated]", invoke(base, "badge"));

        Class<?> cropA = transformAndLoad("dev.polymixin.testgame.CropBlockA");
        assertEquals("BA[annotated]", invoke(cropA, "badge"),
                "@DynamicTargets must reach subclasses without the config declaring an IMixinConfigPlugin");

        assertTrue(GeneratedRegistry.all().stream()
                        .anyMatch(g -> g.configName().equals("polymixin-test3.mixins.json")),
                GeneratedRegistry.all().toString());
    }

    @Test
    @Order(12)
    void ignoresClientOnlyMixinsOnTheServerSide() throws Exception {
        Class<?> base = transformAndLoad("dev.polymixin.testgame.BaseBlock");
        assertEquals("L+extras", invoke(base, "label"),
                "a mixin listed under \"client\" must not apply, and must not be dynamically targeted, on a server");

        assertTrue(GeneratedRegistry.all().stream()
                        .noneMatch(g -> g.originalName().endsWith("MixinClientOnlyBlock")),
                GeneratedRegistry.all().toString());
    }

    @Test
    @Order(11)
    void servesEveryConsumerFromOneSharedScan() throws Exception {
        Class<?> base = transformAndLoad("dev.polymixin.testgame.BaseBlock");
        assertEquals("M[two]", invoke(base, "mark"));

        Class<?> cropA = transformAndLoad("dev.polymixin.testgame.CropBlockA");
        assertEquals("MA[two]", invoke(cropA, "mark"));

        assertEquals(1, ScanCache.scanCount(), "the classpath must be scanned exactly once for all consumers");

        Set<String> configs = new LinkedHashSet<>();
        for (GeneratedMixin generated : GeneratedRegistry.all()) {
            configs.add(generated.configName());
        }
        assertEquals(Set.of("polymixin-test.mixins.json", "polymixin-test2.mixins.json",
                "polymixin-test3.mixins.json"), configs);
    }

    @Test
    @Order(10)
    void accessorMixinsCoverSubclassesWithoutBeingCloned() throws Exception {
        transformAndLoad("dev.polymixin.testgame.BaseBlock");
        Class<?> cropA = transformAndLoad("dev.polymixin.testgame.CropBlockA");

        assertTrue(GeneratedRegistry.all().stream()
                        .noneMatch(g -> g.originalName().endsWith("MixinHardnessAccessor")),
                "interface mixins must not be cloned");

        Class<?> accessor = Class.forName("dev.polymixin.testmixins.MixinHardnessAccessor", false, loader);
        Object instance = cropA.getDeclaredConstructor().newInstance();
        assertTrue(accessor.isInstance(instance),
                "a subclass must inherit the accessor interface from its declared-target superclass");
        assertEquals(3, accessor.getMethod("polymixin$hardness").invoke(instance));
        assertEquals(3, accessor.getMethod("polymixin$computeHardness").invoke(instance));
    }

    @Test
    @Order(19)
    void appliesInterfaceMixinsToImplementersThatDeclareTheMethod() throws Exception {
        transformAndLoad("dev.polymixin.testgame.Growable");

        Class<?> wild = transformAndLoad("dev.polymixin.testgame.WildGrowable");
        assertEquals("wild!", invoke(wild, "grow"));

        Class<?> tended = transformAndLoad("dev.polymixin.testgame.TendedGrowable");
        assertEquals("grow+tended!", invoke(tended, "grow"),
                "delegating to the interface default is no protection: Mixin cannot patch the default,"
                        + " so the implementer needs its own copy");

        Class<?> rooted = transformAndLoad("dev.polymixin.testgame.RootedGrowable");
        assertEquals("grow", invoke(rooted, "grow"),
                "an implementer that declares no override has nothing an injector can match");
    }

    @Test
    @Order(20)
    void generatesInterfaceMixinCopiesForEveryImplementerDeclaringTheMethod() throws Exception {
        transformAndLoad("dev.polymixin.testgame.Growable");

        Set<String> targets = new LinkedHashSet<>();
        for (GeneratedMixin generated : GeneratedRegistry.all()) {
            if (generated.originalName().endsWith("MixinGrowable")) {
                targets.add(generated.targetName());
            }
        }

        assertEquals(Set.of("dev.polymixin.testgame.TendedGrowable", "dev.polymixin.testgame.WildGrowable"),
                targets, targets.toString());
    }
}
