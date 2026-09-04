package dev.polymixin.forge.testmixins;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.polymixin.api.DynamicTargets;
import dev.polymixin.forge.testgame.Target;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Target.class)
//@Debug(export = true)
@DynamicTargets
public interface MixinTarget {
    @WrapMethod(method = "doThing")
    default String wrapDoThing(Operation<String> original) {
        return "Wrap(" + original.call() + ")";
    }
}
