package dev.polymixin.testmixins3;

import dev.polymixin.api.DynamicTargets;
import dev.polymixin.testgame.BaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseBlock.class)
@DynamicTargets
public abstract class MixinAnnotationDriven {

    @Inject(method = "badge()Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 1)
    private void polymixin$badge(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(cir.getReturnValue() + "[annotated]");
    }
}
