package dev.polymixin.testmixins;

import dev.polymixin.testgame.BaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BaseBlock.class)
public interface MixinHardnessAccessor {

    @Accessor("hardness")
    int polymixin$hardness();

    @Invoker("computeHardness")
    int polymixin$computeHardness();
}
