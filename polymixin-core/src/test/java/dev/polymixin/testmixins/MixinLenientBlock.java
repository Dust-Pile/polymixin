package dev.polymixin.testmixins;

import dev.polymixin.testgame.BaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseBlock.class)
public abstract class MixinLenientBlock {

    @Inject(method = "describe()Ljava/lang/String;", at = @At("HEAD"), cancellable = true, require = 1)
    private void polymixin$describe(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("patched");
    }
}
