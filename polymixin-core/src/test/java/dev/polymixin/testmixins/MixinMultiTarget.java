package dev.polymixin.testmixins;

import dev.polymixin.testgame.BaseBlock;
import dev.polymixin.testgame.UnrelatedBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BaseBlock.class, UnrelatedBlock.class})
public abstract class MixinMultiTarget {

    @Inject(method = "tag()Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 1)
    private void polymixin$tag(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(cir.getReturnValue() + "[multi]");
    }
}
