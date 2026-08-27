package dev.polymixin.testmixins;

import dev.polymixin.testgame.BaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseBlock.class)
public abstract class MixinBaseBlock {

    @Inject(method = "canSurvive()Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void polymixin$forceSurvive(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(Boolean.TRUE);
    }
}
