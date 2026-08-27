package dev.polymixin.core.diagnostics;

import dev.polymixin.core.registry.GeneratedMixin;
import dev.polymixin.core.registry.GeneratedRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler.ErrorAction;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PolyMixinErrorHandlerTest {

    private static final class StubMixinInfo implements IMixinInfo {

        private final String className;

        StubMixinInfo(String className) {
            this.className = className;
        }

        @Override
        public IMixinConfig getConfig() {
            return null;
        }

        @Override
        public String getName() {
            return this.className;
        }

        @Override
        public String getClassName() {
            return this.className;
        }

        @Override
        public String getClassRef() {
            return this.className.replace('.', '/');
        }

        @Override
        public byte[] getClassBytes() {
            return new byte[0];
        }

        @Override
        public boolean isDetachedSuper() {
            return false;
        }

        @Override
        public ClassNode getClassNode(int flags) {
            return null;
        }

        @Override
        public List<String> getTargetClasses() {
            return List.of();
        }

        @Override
        public int getPriority() {
            return 1000;
        }

        @Override
        public Phase getPhase() {
            return Phase.DEFAULT;
        }
    }

    @Test
    void downgradesGeneratedMixinsOnly() {
        GeneratedRegistry.register(track(new GeneratedMixin("cfg", "com.example.MixinBush",
                "com.example.MixinBush_pm_Crop_1234", "MixinBush_pm_Crop_1234", "com.other.Crop", List.of(), new byte[0])));

        PolyMixinErrorHandler handler = new PolyMixinErrorHandler();
        RuntimeException cause = new RuntimeException("boom");

        assertEquals(ErrorAction.WARN, handler.onApplyError("com.other.Crop", cause,
                new StubMixinInfo("com.example.MixinBush_pm_Crop_1234"), ErrorAction.ERROR));

        assertEquals(ErrorAction.ERROR, handler.onApplyError("net.minecraft.Bush", cause,
                new StubMixinInfo("com.example.MixinBush"), ErrorAction.ERROR));
    }

    @Test
    void recordsTheFailureForTheSummary() {
        GeneratedMixin generated = new GeneratedMixin("cfg", "com.example.MixinBush",
                "com.example.MixinBush_pm_Crop_5678", "MixinBush_pm_Crop_5678", "com.other.Crop2", List.of(), new byte[0]);
        GeneratedRegistry.register(track(generated));

        new PolyMixinErrorHandler().onApplyError("com.other.Crop2", new IllegalStateException("nope"),
                new StubMixinInfo("com.example.MixinBush_pm_Crop_5678"), ErrorAction.ERROR);

        assertEquals("IllegalStateException: nope", generated.failure());
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
