package dev.polymixin.testmixins;

import dev.polymixin.testgame.UnrelatedBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(UnrelatedBlock.class)
public abstract class MixinNoRequire {

    @Inject(method = "canSurvive()Z", at = @At("HEAD"))
    private void polymixin$noRequire(CallbackInfoReturnable<Boolean> cir) {
    }
}
