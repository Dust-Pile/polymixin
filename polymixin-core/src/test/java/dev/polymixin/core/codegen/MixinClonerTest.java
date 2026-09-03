package dev.polymixin.core.codegen;

import dev.polymixin.testenv.TestMixinService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinClonerTest {

    private static final String MIXIN_DESC = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String INJECT_DESC = "Lorg/spongepowered/asm/mixin/injection/Inject;";

    private static ClassNode read(String name) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(TestMixinService.readBytes(name)).accept(node, 0);
        return node;
    }

    private static ClassNode readBytes(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    @Test
    void rewritesNameTargetAndRequirements() throws Exception {
        ClassNode source = read("dev.polymixin.testmixins.MixinBaseBlock");
        String generatedRef = source.name + "_pm_CropBlockA_deadbeef";

        byte[] bytes = MixinCloner.clone(source, generatedRef, "dev/polymixin/testgame/CropBlockA", false, true, null).bytes();
        ClassNode generated = readBytes(bytes);

        assertEquals(generatedRef, generated.name);

        AnnotationNode mixin = AnnotationNodes.find(generated.invisibleAnnotations, MIXIN_DESC);
        assertNotNull(mixin);
        List<?> value = (List<?>) AnnotationNodes.get(mixin, "value");
        assertEquals(1, value.size());
        assertEquals(Type.getObjectType("dev/polymixin/testgame/CropBlockA"), value.get(0));
        assertNull(AnnotationNodes.get(mixin, "targets"));

        MethodNode injector = generated.methods.stream()
                .filter(m -> m.name.equals("polymixin$forceSurvive"))
                .findFirst()
                .orElseThrow();
        AnnotationNode inject = AnnotationNodes.find(injector.visibleAnnotations, INJECT_DESC);
        assertNotNull(inject);
        assertEquals(0, AnnotationNodes.get(inject, "require"));
        assertEquals(0, AnnotationNodes.get(inject, "expect"));
    }

    @Test
    void leavesTheOriginalStrict() throws Exception {
        ClassNode source = read("dev.polymixin.testmixins.MixinBaseBlock");
        MixinCloner.clone(source, source.name + "_pm_x", "dev/polymixin/testgame/CropBlockA", false, true, null);

        ClassNode reread = read("dev.polymixin.testmixins.MixinBaseBlock");
        MethodNode injector = reread.methods.stream()
                .filter(m -> m.name.equals("polymixin$forceSurvive"))
                .findFirst()
                .orElseThrow();
        AnnotationNode inject = AnnotationNodes.find(injector.visibleAnnotations, INJECT_DESC);
        assertEquals(1, AnnotationNodes.get(inject, "require"));
    }

    @Test
    void keepsRequirementsWhenRelaxationDisabled() throws Exception {
        ClassNode source = read("dev.polymixin.testmixins.MixinBaseBlock");
        byte[] bytes = MixinCloner.clone(source, source.name + "_pm_y", "dev/polymixin/testgame/CropBlockB", false, false, null).bytes();
        ClassNode generated = readBytes(bytes);

        MethodNode injector = generated.methods.stream()
                .filter(m -> m.name.equals("polymixin$forceSurvive"))
                .findFirst()
                .orElseThrow();
        AnnotationNode inject = AnnotationNodes.find(injector.visibleAnnotations, INJECT_DESC);
        assertEquals(1, AnnotationNodes.get(inject, "require"));
    }

    @Test
    void remapsSelfReferences() throws Exception {
        ClassNode source = read("dev.polymixin.testmixins.MixinBaseBlock");
        String generatedRef = source.name + "_pm_self";
        byte[] bytes = MixinCloner.clone(source, generatedRef, "dev/polymixin/testgame/CropBlockA", false, true, null).bytes();

        String text = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(text.contains(generatedRef), "generated class must reference its own new name");
    }

    private static final class FakeMembers implements TargetMembers {

        private final Set<String> declared;
        private final Set<String> inherited;

        FakeMembers(Set<String> declared, Set<String> inherited) {
            this.declared = declared;
            this.inherited = inherited;
        }

        @Override
        public boolean declaresMethodNamed(String name) {
            return this.declared.stream().anyMatch(d -> d.startsWith(name));
        }

        @Override
        public boolean declaresMethod(String name, String desc) {
            return this.declared.contains(name + desc);
        }

        @Override
        public boolean declaresField(String name, String desc) {
            return this.declared.contains(name + desc);
        }

        @Override
        public boolean inheritsAccessibleMethod(String name, String desc) {
            return this.inherited.contains(name + desc);
        }

        @Override
        public boolean inheritsAccessibleField(String name, String desc) {
            return this.inherited.contains(name + desc);
        }
    }

    @Test
    void skipsAccessorInterfaceMixinsBecauseSubclassesAlreadyInheritThem() throws Exception {
        ClassNode source = read("dev.polymixin.testmixins.MixinHardnessAccessor");
        CloneResult result = MixinCloner.clone(source, source.name + "_pm_a", "dev/polymixin/testgame/CropBlockA",
                false, true, null);

        assertTrue(result.isSkipped());
        assertTrue(result.skipReason().contains("accessor mixin"), result.skipReason());
    }

    @Test
    void turnsAnInterfaceMixinIntoAClassMixinWhenTheTargetIsAClass() throws Exception {
        ClassNode source = read("dev.polymixin.testmixins3.MixinGrowable");
        CloneResult result = MixinCloner.clone(source, source.name + "_pm_e", "dev/polymixin/testgame/WildGrowable",
                false, true, null);

        assertFalse(result.isSkipped(), String.valueOf(result.skipReason()));
        ClassNode generated = readBytes(result.bytes());
        assertEquals(0, generated.access & Opcodes.ACC_INTERFACE);
        assertEquals("java/lang/Object", generated.superName);
    }

    @Test
    void keepsAnInterfaceMixinAnInterfaceWhenTheTargetIsOne() throws Exception {
        ClassNode source = read("dev.polymixin.testmixins3.MixinGrowable");
        CloneResult result = MixinCloner.clone(source, source.name + "_pm_f", "dev/polymixin/testgame/Growable",
                true, true, null);

        assertFalse(result.isSkipped(), String.valueOf(result.skipReason()));
        assertNotEquals(0, readBytes(result.bytes()).access & Opcodes.ACC_INTERFACE);
    }

    @Test
    void stripsShadowsTheTargetInheritsRatherThanDeclares() throws Exception {
        ClassNode source = read("dev.polymixin.testmixins.MixinShadowBlock");
        CloneResult result = MixinCloner.clone(source, source.name + "_pm_b", "dev/polymixin/testgame/CropBlockA",
                false, true, new FakeMembers(Set.of(), Set.of("computeHardness()I", "hardnessI")));

        assertFalse(result.isSkipped(), String.valueOf(result.skipReason()));
        assertEquals(Set.of("field hardness", "method computeHardness"), Set.copyOf(result.strippedShadows()));

        ClassNode generated = readBytes(result.bytes());
        assertTrue(generated.fields.stream().noneMatch(f -> f.name.equals("hardness")));
        assertTrue(generated.methods.stream().noneMatch(m -> m.name.equals("computeHardness")));
    }

    @Test
    void keepsShadowsTheTargetDeclares() throws Exception {
        ClassNode source = read("dev.polymixin.testmixins.MixinShadowBlock");
        CloneResult result = MixinCloner.clone(source, source.name + "_pm_c", "dev/polymixin/testgame/CropBlockA",
                false, true, new FakeMembers(Set.of("computeHardness()I", "hardnessI"), Set.of()));

        assertFalse(result.isSkipped());
        assertEquals(List.of(), result.strippedShadows());
        ClassNode generated = readBytes(result.bytes());
        assertTrue(generated.fields.stream().anyMatch(f -> f.name.equals("hardness")));
    }

    @Test
    void skipsTargetsThatCannotReachAShadowedMember() throws Exception {
        ClassNode source = read("dev.polymixin.testmixins.MixinShadowBlock");
        CloneResult result = MixinCloner.clone(source, source.name + "_pm_d", "dev/polymixin/testgame/CropBlockA",
                false, true, new FakeMembers(Set.of(), Set.of()));

        assertTrue(result.isSkipped());
        assertTrue(result.skipReason().contains("@Shadow"), result.skipReason());
    }
}
