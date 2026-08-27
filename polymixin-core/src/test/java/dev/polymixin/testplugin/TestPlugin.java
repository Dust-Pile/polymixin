package dev.polymixin.testplugin;

import dev.polymixin.api.DynamicTargetProvider;
import dev.polymixin.api.TargetContext;
import io.github.classgraph.ClassInfo;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class TestPlugin implements IMixinConfigPlugin, DynamicTargetProvider {

    public static final List<String> POST_APPLIED = new ArrayList<>();
    public static volatile TargetContext LAST_CONTEXT;

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        POST_APPLIED.add(mixinClassName + " -> " + targetClassName);
    }

    @Override
    public Collection<ClassInfo> provideTargets(TargetContext ctx) {
        LAST_CONTEXT = ctx;
        return ctx.subclassesOfDeclaredTargets();
    }
}
