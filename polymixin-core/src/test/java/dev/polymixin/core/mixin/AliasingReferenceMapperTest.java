package dev.polymixin.core.mixin;

import dev.polymixin.core.registry.GeneratedMixin;
import dev.polymixin.core.registry.GeneratedRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AliasingReferenceMapperTest {

    private static final class RecordingMapper implements IReferenceMapper {

        final List<String> seen = new ArrayList<>();
        String context;

        @Override
        public boolean isDefault() {
            return false;
        }

        @Override
        public String getResourceName() {
            return "recording";
        }

        @Override
        public String getStatus() {
            return "recording";
        }

        @Override
        public String getContext() {
            return this.context;
        }

        @Override
        public void setContext(String context) {
            this.context = context;
        }

        @Override
        public String remap(String className, String reference) {
            this.seen.add(className);
            return "canSurvive".equals(reference) && "com/example/mixin/MixinBush".equals(className)
                    ? "m_7898_"
                    : reference;
        }

        @Override
        public String remapWithContext(String context, String className, String reference) {
            return this.remap(className, reference);
        }
    }

    @Test
    void rewritesGeneratedClassRefBackToTheOriginal() {
        GeneratedRegistry.register(track(new GeneratedMixin("cfg", "com.example.mixin.MixinBush",
                "com.example.mixin.MixinBush_pm_Crop_1234", "MixinBush_pm_Crop_1234",
                "com.other.Crop", List.of("net.minecraft.Bush"), new byte[0])));

        RecordingMapper delegate = new RecordingMapper();
        IReferenceMapper mapper = AliasingReferenceMapper.wrap(delegate);

        assertEquals("m_7898_", mapper.remap("com/example/mixin/MixinBush_pm_Crop_1234", "canSurvive"));
        assertEquals("com/example/mixin/MixinBush", delegate.seen.get(0));
    }

    @Test
    void leavesUnknownClassRefsAlone() {
        RecordingMapper delegate = new RecordingMapper();
        IReferenceMapper mapper = AliasingReferenceMapper.wrap(delegate);

        assertEquals("canSurvive", mapper.remap("com/other/Mixin", "canSurvive"));
        assertEquals("com/other/Mixin", delegate.seen.get(0));
    }

    @Test
    void retargetsAnOwnerTheAnnotationProcessorAddedForTheDeclaredTarget() {
        GeneratedMixin generated = new GeneratedMixin("cfg", "com.example.mixin.MixinBush",
                "com.example.mixin.MixinBush_pm_Crop_9999", "MixinBush_pm_Crop_9999",
                "com.other.Crop", List.of("net.minecraft.Bush"), new byte[0]);

        assertEquals("Lcom/other/Crop;m_7898_()Z",
                AliasingReferenceMapper.retargetOwner("canSurvive", "Lnet/minecraft/Bush;m_7898_()Z", generated));
    }

    @Test
    void leavesOwnersTheAuthorWroteAlone() {
        GeneratedMixin generated = new GeneratedMixin("cfg", "com.example.mixin.MixinBush",
                "com.example.mixin.MixinBush_pm_Crop_9999", "MixinBush_pm_Crop_9999",
                "com.other.Crop", List.of("net.minecraft.Bush"), new byte[0]);

        assertEquals("Lnet/minecraft/Bush;m_7898_()Z",
                AliasingReferenceMapper.retargetOwner("Lnet/minecraft/Bush;canSurvive()Z",
                        "Lnet/minecraft/Bush;m_7898_()Z", generated));
        assertEquals("Lnet/minecraft/LevelReader;m_8055_()V",
                AliasingReferenceMapper.retargetOwner("getBlockState",
                        "Lnet/minecraft/LevelReader;m_8055_()V", generated));
    }

    @Test
    void doesNotDoubleWrap() {
        RecordingMapper delegate = new RecordingMapper();
        IReferenceMapper once = AliasingReferenceMapper.wrap(delegate);
        assertEquals(once, AliasingReferenceMapper.wrap(once));
    }

    private final List<GeneratedMixin> tracked = new ArrayList<>();

    private GeneratedMixin track(GeneratedMixin mixin) {
        this.tracked.add(mixin);
        return mixin;
    }

    @AfterEach
    void unregisterTracked() {
        this.tracked.forEach(GeneratedRegistry::unregister);
        this.tracked.clear();
    }
}
