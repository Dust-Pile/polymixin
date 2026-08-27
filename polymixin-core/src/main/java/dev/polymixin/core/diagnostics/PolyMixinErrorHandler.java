package dev.polymixin.core.diagnostics;

import dev.polymixin.core.registry.GeneratedMixin;
import dev.polymixin.core.registry.GeneratedRegistry;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class PolyMixinErrorHandler implements IMixinErrorHandler {

    @Override
    public ErrorAction onPrepareError(IMixinConfig config, Throwable th, IMixinInfo mixin, ErrorAction action) {
        return downgrade("prepare", mixin, th, action);
    }

    @Override
    public ErrorAction onApplyError(String targetClassName, Throwable th, IMixinInfo mixin, ErrorAction action) {
        return downgrade("apply", mixin, th, action);
    }

    private ErrorAction downgrade(String phase, IMixinInfo mixin, Throwable th, ErrorAction action) {
        if (mixin == null) {
            return action;
        }
        GeneratedMixin generated = GeneratedRegistry.byGeneratedName(mixin.getClassName());
        if (generated == null) {
            return action;
        }
        generated.markFailed(th == null ? "unknown" : th.getClass().getSimpleName() + ": " + th.getMessage());
        if (action == ErrorAction.WARN || action == ErrorAction.NONE) {
            Log.warn("dynamic target failed during {}: {} on {} ({})",
                    phase, generated.originalName(), generated.targetName(), describe(th));
            return action;
        }
        Log.warn("downgrading {} -> WARN for dynamic target: {} on {} ({})",
                action, generated.originalName(), generated.targetName(), describe(th));
        return ErrorAction.WARN;
    }

    private static String describe(Throwable th) {
        if (th == null) {
            return "no detail";
        }
        return th.getClass().getName() + ": " + th.getMessage();
    }
}
