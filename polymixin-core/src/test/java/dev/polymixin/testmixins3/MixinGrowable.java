package dev.polymixin.testmixins3;

import dev.polymixin.api.DynamicTargets;
import dev.polymixin.testgame.Growable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Growable.class)
@DynamicTargets
public interface MixinGrowable {

    @Inject(method = "grow()Ljava/lang/String;", at = @At("RETURN"), cancellable = true, require = 1)
    default void polymixin$grow(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(cir.getReturnValue() + "!");
    }
}
