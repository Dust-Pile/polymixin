package dev.polymixin.testmixins3;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.polymixin.api.DynamicTargets;
import dev.polymixin.testgame.BaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BaseBlock.class)
@DynamicTargets
public abstract class MixinSuperChain {

    @ModifyReturnValue(method = "chain()Ljava/lang/String;", at = @At("RETURN"), require = 1)
    private String polymixin$mark(String original) {
        return original + "!";
    }
}
