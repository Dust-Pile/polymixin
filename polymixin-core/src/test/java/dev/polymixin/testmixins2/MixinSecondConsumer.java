package dev.polymixin.testmixins2;

import dev.polymixin.testgame.BaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseBlock.class)
public abstract class MixinSecondConsumer {

    @Inject(method = "mark()Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 1)
    private void polymixin$mark(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(cir.getReturnValue() + "[two]");
    }
}
