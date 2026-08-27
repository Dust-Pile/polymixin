package dev.polymixin.testmixins;

import dev.polymixin.testgame.BaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseBlock.class)
public abstract class MixinShadowBlock {

    @Shadow
    protected int hardness;

    @Shadow
    protected abstract int computeHardness();

    @Inject(method = "effectiveHardness()I", at = @At("HEAD"), cancellable = true, require = 1)
    private void polymixin$overrideHardness(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.computeHardness() + this.hardness + 100);
    }
}
