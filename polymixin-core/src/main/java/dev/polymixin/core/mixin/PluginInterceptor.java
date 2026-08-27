package dev.polymixin.core.mixin;

import dev.polymixin.core.diagnostics.Log;
import dev.polymixin.core.diagnostics.Summary;
import dev.polymixin.core.platform.Platform;
import dev.polymixin.core.registry.GeneratedMixin;
import dev.polymixin.core.registry.GeneratedRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class PluginInterceptor implements IMixinConfigPlugin {

    private final IMixinConfigPlugin delegate;
    private final String configName;

    private PluginInterceptor(IMixinConfigPlugin delegate, String configName) {
        this.delegate = delegate;
        this.configName = configName;
    }

    public static IMixinConfigPlugin wrap(IMixinConfigPlugin delegate, String configName) {
        if (delegate instanceof PluginInterceptor) {
            return delegate;
        }
        return new PluginInterceptor(delegate, configName);
    }

    public static IMixinConfigPlugin unwrap(IMixinConfigPlugin plugin) {
        return plugin instanceof PluginInterceptor ? ((PluginInterceptor) plugin).delegate : plugin;
    }

    public static boolean isInstalled(IMixinConfigPlugin plugin) {
        return plugin instanceof PluginInterceptor;
    }

    @Override
    public void onLoad(String mixinPackage) {
        if (this.delegate != null) {
            this.delegate.onLoad(mixinPackage);
        }
    }

    @Override
    public String getRefMapperConfig() {
        return this.delegate == null ? null : this.delegate.getRefMapperConfig();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (this.delegate == null) {
            return true;
        }
        GeneratedMixin generated = GeneratedRegistry.byGeneratedName(mixinClassName);
        return this.delegate.shouldApplyMixin(targetClassName,
                generated != null ? generated.originalName() : mixinClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
        if (this.delegate != null) {
            this.delegate.acceptTargets(myTargets, otherTargets);
        }
    }

    @Override
    public List<String> getMixins() {
        List<String> merged = new ArrayList<>();
        List<String> own = this.delegate == null ? null : this.delegate.getMixins();
        if (own != null) {
            merged.addAll(own);
        }
        for (GeneratedMixin generated : GeneratedRegistry.forConfig(this.configName)) {
            merged.add(generated.relativeName());
        }
        return merged;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (this.delegate != null) {
            this.delegate.preApply(targetClassName, targetClass, mixinClassName, mixinInfo);
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        Summary.noteApplied(mixinClassName);
        if (Boolean.getBoolean(Platform.PROP_DEBUG) && GeneratedRegistry.byGeneratedName(mixinClassName) != null) {
            Log.info("  applied {} to {}", mixinClassName, targetClassName);
        }
        if (this.delegate != null) {
            this.delegate.postApply(targetClassName, targetClass, mixinClassName, mixinInfo);
        }
    }
}
