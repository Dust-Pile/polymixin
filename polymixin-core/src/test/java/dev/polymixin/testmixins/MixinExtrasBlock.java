package dev.polymixin.testmixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.polymixin.testgame.BaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BaseBlock.class)
public abstract class MixinExtrasBlock {

    @ModifyReturnValue(method = "label()Ljava/lang/String;", at = @At("RETURN"), require = 1)
    private String polymixin$appendMarker(String original) {
        return original + "+extras";
    }
}
