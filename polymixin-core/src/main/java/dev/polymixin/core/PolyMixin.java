package dev.polymixin.core;

import dev.polymixin.api.DynamicTargetProvider;
import dev.polymixin.api.TargetContext;
import dev.polymixin.api.TargetInspector;
import dev.polymixin.core.codegen.*;
import dev.polymixin.core.diagnostics.*;
import dev.polymixin.core.mixin.*;
import dev.polymixin.core.platform.Dependents;
import dev.polymixin.core.platform.Platform;
import dev.polymixin.core.registry.GeneratedMixin;
import dev.polymixin.core.registry.GeneratedRegistry;
import dev.polymixin.core.scan.ScanCache;
import dev.polymixin.core.scan.SuperDelegation;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.MixinService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

public final class PolyMixin {

    public static final String PROP_DISABLE = "polymixin.disable";

    private static final Map<String, IMixinConfigPlugin> EXPLICIT = new LinkedHashMap<>();

    private static boolean bootstrapped;
    private static boolean ran;
    private static int inspected;
    private static int annotationReads;

    private PolyMixin() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        Mixins.registerErrorHandlerClass(PolyMixinErrorHandler.class.getName());
        Diagnostics.checkGlobalHooks();
    }

    public static synchronized void register(String configName, IMixinConfigPlugin plugin) {
        EXPLICIT.put(configName, plugin);
    }

    public static synchronized void run(Set<String> myTargets, Set<String> otherTargets) {
        if (ran) {
            return;
        }
        ran = true;
        if (Boolean.getBoolean(PROP_DISABLE)) {
            Log.info("disabled by -D{}", PROP_DISABLE);
            return;
        }
        try {
            execute(myTargets, otherTargets);
        } catch (Throwable th) {
            Log.error("dynamic targeting aborted: {}", th);
        }
    }

    private static void execute(Set<String> myTargets, Set<String> otherTargets) {
        long start = System.nanoTime();
        List<Candidate> candidates = discover(myTargets, otherTargets);
        long discoveryMillis = (System.nanoTime() - start) / 1_000_000L;
        String detail = Log.format("discovery [{}]: {} mixin(s) seen, {} read for @DynamicTargets,"
                        + " {} opted in, {} ms",
                Dependents.mode(), inspected, annotationReads, candidates.size(), discoveryMillis);
        if (Boolean.getBoolean(Platform.PROP_DEBUG)) {
            Log.info("{}", detail);
        } else {
            Log.debug("{}", detail);
        }
        if (candidates.isEmpty()) {
            Log.debug("no mixin opted in via @DynamicTargets or DynamicTargetProvider, skipping scan");
            return;
        }
        if (discoveryMillis > 250L) {
            Log.warn("{}", detail);
        }

        List<Path> roots = Platform.roots();
        if (roots.isEmpty()) {
            Log.warn("{} dynamic provider(s) present but no classpath source is available; "
                            + "dynamic targets will not be added",
                    candidates.size());
            return;
        }
        Log.info("platform {} contributed {} classpath roots", Platform.source().platformName(), roots.size());

        ScanResult scan = ScanCache.scan(roots);
        Map<IMixinConfig, List<GeneratedMixin>> pending = new LinkedHashMap<>();

        for (Candidate candidate : candidates) {
            generate(candidate, scan, pending);
        }

        int installed = 0;
        for (Map.Entry<IMixinConfig, List<GeneratedMixin>> entry : pending.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            if (install(entry.getKey())) {
                for (GeneratedMixin generated : entry.getValue()) {
                    GeneratedRegistry.register(generated);
                }
                installed += entry.getValue().size();
            } else {
                Log.error("could not install hooks on config {}; {} dynamic target(s) dropped",
                        entry.getKey().getName(), entry.getValue().size());
            }
        }

        Log.info("registered {} generated mixin(s) across {} config(s)", installed, pending.size());
        for (GeneratedMixin generated : GeneratedRegistry.all()) {
            Log.debug("  {} -> {}", generated.generatedName(), generated.targetName());
        }
        if (installed > 0) {
            Summary.arm();
        }
    }

    private static List<Candidate> discover(Set<String> myTargets, Set<String> otherTargets) {
        Set<String> allTargets = new LinkedHashSet<>();
        allTargets.addAll(myTargets);
        allTargets.addAll(otherTargets);

        Map<String, Candidate> found = new LinkedHashMap<>();
        Set<String> seen = new LinkedHashSet<>();
        for (String target : allTargets) {
            for (IMixinInfo info : MixinRegistryAccess.mixinsTargeting(target)) {
                IMixinConfig config = info.getConfig();
                if (config == null) {
                    continue;
                }
                if (!seen.add(info.getClassName())) {
                    continue;
                }
                Candidate candidate = fromPlugin(info, config);
                if (candidate == null) {
                    candidate = fromAnnotation(info, config);
                }
                if (candidate != null) {
                    found.put(info.getClassName(), candidate);
                }
            }
        }
        inspected = seen.size();
        return new ArrayList<>(found.values());
    }

    private static Candidate fromPlugin(IMixinInfo info, IMixinConfig config) {
        IMixinConfigPlugin plugin = resolvePlugin(config);
        if (!(plugin instanceof DynamicTargetProvider)) {
            return null;
        }
        DynamicTargetProvider provider = (DynamicTargetProvider) plugin;
        return new Candidate(info, config, provider, provider.relaxInjectionRequirements());
    }

    private static Candidate fromAnnotation(IMixinInfo info, IMixinConfig config) {
        if (!Dependents.mayDeclareAnnotation(ConfigOwner.modId(config))) {
            return null;
        }
        annotationReads++;
        AnnotationNode annotation = DynamicTargetsAnnotation.find(info);
        if (annotation == null) {
            return null;
        }
        DynamicTargetProvider provider = DynamicTargetsAnnotation.provider(annotation, info.getClassName());
        if (provider == null) {
            return null;
        }
        return new Candidate(info, config, provider,
                DynamicTargetsAnnotation.relaxInjectionRequirements(annotation));
    }

    private static IMixinConfigPlugin resolvePlugin(IMixinConfig config) {
        IMixinConfigPlugin explicit = EXPLICIT.get(config.getName());
        if (explicit != null) {
            return explicit;
        }
        return PluginInterceptor.unwrap(config.getPlugin());
    }

    private static void generate(Candidate candidate, ScanResult scan,
            Map<IMixinConfig, List<GeneratedMixin>> pending) {
        IMixinInfo info = candidate.info;
        IMixinConfig config = candidate.config;

        List<String> declared = new ArrayList<>();
        for (String name : info.getTargetClasses()) {
            declared.add(name.replace('/', '.'));
        }

        Summary.MixinStats stats = Summary.stats(info.getClassName());
        stats.configName(config.getName());
        stats.declared(declared);

        if (config.isRequired()) {
            Log.warn("config {} is marked \"required\": a dynamic target that fails to apply will abort the"
                    + " whole target class instead of being downgraded to a warning", config.getName());
        }

        ClassNode source;
        try {
            source = info.getClassNode(0);
        } catch (Throwable th) {
            Log.error("could not read bytecode of {}: {}", info.getClassName(), th);
            return;
        }
        RequireAudit.audit(info.getClassName(), source);

        Collection<ClassInfo> provided;
        try {
            provided = candidate.provider.provideTargets(
                    new TargetContext(config.getName(), info.getClassName(), declared, scan,
                            inspector(scan, config, info.getClassRef(), targetsConstructor(source),
                                    InjectorSelectors.of(source))));
        } catch (Throwable th) {
            Log.error("provider for {} threw: {}", info.getClassName(), th);
            return;
        }
        if (provided == null) {
            return;
        }
        stats.discovered(provided.size());

        String mixinPackage = config.getMixinPackage();
        if (mixinPackage == null || mixinPackage.isEmpty()) {
            Log.error("config {} has no mixin package, cannot host generated mixins", config.getName());
            return;
        }
        if (!mixinPackage.endsWith(".")) {
            mixinPackage = mixinPackage + ".";
        }

        boolean sourceIsInterface = (source.access & Opcodes.ACC_INTERFACE) != 0;
        if ((sourceIsInterface && InjectorSelectors.anyPresent(source)) && (!Platform.isFabricMixin() || !Platform.injectorsInInterfaces())) {
            detachFromDeclaredInterfaces(info, config, declared, scan);
        }
        IClassTracker tracker = MixinService.getService().getClassTracker();
        Set<String> seen = new LinkedHashSet<>(declared);
        List<GeneratedMixin> generated = pending.computeIfAbsent(config, k -> new ArrayList<>());

        for (ClassInfo target : provided) {
            String targetName = target.getName();
            if (!seen.add(targetName)) {
                stats.skip("duplicate/declared");
                continue;
            }
            if (target.isAnnotation() || (target.isInterface() && !sourceIsInterface)) {
                stats.skip("not a class");
                continue;
            }
            if (targetName.startsWith(mixinPackage)) {
                stats.skip("inside mixin package");
                continue;
            }
            if (tracker != null && tracker.isClassLoaded(targetName)) {
                Log.warn("skipping {} for {}: already class-loaded", targetName, info.getClassName());
                stats.skip("already class-loaded");
                continue;
            }

            String suffix = suffixFor(info.getClassRef(), targetName);
            String generatedName = info.getClassName() + suffix;
            if (!generatedName.startsWith(mixinPackage)) {
                Log.warn("skipping {} for {}: mixin is outside its config package {}",
                        targetName, info.getClassName(), mixinPackage);
                stats.skip("outside mixin package");
                continue;
            }

            CloneResult result;
            try {
                result = MixinCloner.clone(source, info.getClassRef() + suffix, targetName.replace('.', '/'),
                        target.isInterface(), candidate.relaxInjectionRequirements,
                        MixinClassInfoMembers.of(targetName.replace('.', '/')));
            } catch (Throwable th) {
                Log.error("codegen failed for {} -> {}: {}", info.getClassName(), targetName, th);
                stats.skip("codegen failed");
                continue;
            }
            if (result.isSkipped()) {
                Log.debug("skipping {} for {}: {}", targetName, info.getClassName(), result.skipReason());
                stats.skip(result.skipReason());
                continue;
            }
            if (!result.strippedShadows().isEmpty()) {
                Log.debug("{} -> {}: resolving inherited {} through the target's hierarchy",
                        info.getClassName(), targetName, result.strippedShadows());
            }

            generated.add(new GeneratedMixin(config.getName(), info.getClassName(), generatedName,
                    generatedName.substring(mixinPackage.length()), targetName, declared, result.bytes()));
            if (Boolean.getBoolean(Platform.PROP_DEBUG)) {
                Log.info("  generated {} for {}", generatedName, targetName);
            }
        }

        Set<String> inner = MixinCloner.innerClassesOf(source);
        if (!inner.isEmpty()) {
            Log.warn("{} declares inner classes {}; generated copies reuse the originals, which Mixin"
                            + " conforms per target. Verify behaviour if these are non-static or capture state.",
                    info.getClassName(), inner);
        }
    }

    /**
     * Mixin refuses to apply an injector that lives in an interface mixin, so the mixin can never
     * patch the interface it declares, and leaving it attached only produces a failed apply. The
     * generated copies carry the injectors onto the implementers instead.
     */
    private static void detachFromDeclaredInterfaces(IMixinInfo info, IMixinConfig config,
            List<String> declared, ScanResult scan) {
        for (String target : declared) {
            ClassInfo targetInfo = scan.getClassInfo(target);
            if (targetInfo == null || !targetInfo.isInterface()) {
                continue;
            }
            try {
                if (!MixinInternals.detachTarget(config, info, target)) {
                    continue;
                }
            } catch (Throwable th) {
                Log.warn("could not detach {} from the interface {}; Mixin will log a failed apply for it: {}",
                        info.getClassName(), target, th);
                continue;
            }
            Log.info("{} declares injectors and targets the interface {}; Mixin cannot apply those to an"
                            + " interface, so only the generated copies on its implementers will run",
                    info.getClassName(), target);
        }
    }

    private static TargetInspector inspector(ScanResult scan, IMixinConfig config, String mixinClassRef,
            boolean constructorTargeted, Set<String> selectors) {
        return new TargetInspector() {

            @Override
            public String resolveMethodName(String methodName) {
                try {
                    IReferenceMapper mapper = MixinInternals.getReferenceMapper(config);
                    if (mapper == null) {
                        return methodName;
                    }
                    return memberNameOf(mapper.remap(mixinClassRef, methodName), methodName);
                } catch (Throwable th) {
                    return methodName;
                }
            }

            @Override
            public boolean declaresMethod(String className, String methodName) {
                TargetMembers members = MixinClassInfoMembers.of(className.replace('.', '/'));
                return members != null && members.declaresMethodNamed(methodName);
            }

            @Override
            public boolean delegatesToSuper(String className, String methodName) {
                return SuperDelegation.delegatesToSuper(scan, className, methodName);
            }

            @Override
            public boolean bypassesAnyOverride(String className) {
                TargetMembers members = MixinClassInfoMembers.of(className.replace('.', '/'));
                if (members == null) {
                    return true;
                }
                return SuperDelegation.bypassesAnyOverride(scan, className, members::inheritsAccessibleMethod);
            }

            @Override
            public boolean mixinTargetsConstructor() {
                return constructorTargeted;
            }

            @Override
            public Set<String> injectedMethodNames() {
                if (selectors == null) {
                    return null;
                }
                Set<String> names = new LinkedHashSet<>();
                for (String selector : selectors) {
                    names.add(this.resolveMethodName(selector));
                }
                return names;
            }
        };
    }

    static boolean targetsConstructor(ClassNode mixin) {
        for (MethodNode method : mixin.methods) {
            if (mentionsConstructor(method.visibleAnnotations) || mentionsConstructor(method.invisibleAnnotations)) {
                return true;
            }
        }
        return false;
    }

    private static boolean mentionsConstructor(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return false;
        }
        for (AnnotationNode annotation : annotations) {
            Object value = AnnotationNodes.get(annotation, "method");
            if (value instanceof List) {
                for (Object entry : (List<?>) value) {
                    if (String.valueOf(entry).contains("<init>")) {
                        return true;
                    }
                }
            } else if (value != null && String.valueOf(value).contains("<init>")) {
                return true;
            }
        }
        return false;
    }

    static String memberNameOf(String remapped, String fallback) {
        if (remapped == null || remapped.isEmpty()) {
            return fallback;
        }
        if (remapped.charAt(0) != 'L') {
            int paren = remapped.indexOf('(');
            return paren > 0
                   ? remapped.substring(0, paren)
                   : remapped;
        }
        int semi = remapped.indexOf(';');
        if (semi < 0) {
            return fallback;
        }
        String tail = remapped.substring(semi + 1);
        int paren = tail.indexOf('(');
        String name = paren > 0
                      ? tail.substring(0, paren)
                      : tail;
        return name.isEmpty()
               ? fallback
               : name;
    }

    private static boolean install(IMixinConfig config) {
        Diagnostics.checkConfigHooks(config);
        try {
            MixinInternals.setService(config, ServiceInterceptor.wrap(MixinInternals.getService(config)));
        } catch (Throwable th) {
            Log.error("could not wrap the mixin service for {}: {}", config.getName(), th);
            return false;
        }
        try {
            Object refMapper = MixinInternals.getReferenceMapper(config);
            if (refMapper != null) {
                MixinInternals.setReferenceMapper(config,
                        AliasingReferenceMapper.wrap(MixinInternals.getReferenceMapper(config)));
            }
        } catch (Throwable th) {
            Log.error("could not alias the reference mapper for {}; soft-remapped references in generated"
                            + " mixins would silently fail to resolve, so no dynamic targets will be added: {}",
                    config.getName(), th);
            return false;
        }
        try {
            Object handle = MixinInternals.getPluginHandle(config);
            MixinInternals.setPlugin(handle, PluginInterceptor.wrap(config.getPlugin(), config.getName()));
        } catch (Throwable th) {
            Log.error("could not wrap the config plugin for {}: {}", config.getName(), th);
            return false;
        }
        return true;
    }

    private static String suffixFor(String mixinRef, String targetName) {
        String simple = targetName.substring(targetName.lastIndexOf('.') + 1).replace('$', '_');
        return "_pm_" + simple + "_" + shortHash(mixinRef + ':' + targetName);
    }

    private static String shortHash(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception ex) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private static final class Candidate {

        final IMixinInfo info;
        final IMixinConfig config;
        final DynamicTargetProvider provider;
        final boolean relaxInjectionRequirements;

        Candidate(IMixinInfo info, IMixinConfig config, DynamicTargetProvider provider,
                boolean relaxInjectionRequirements) {
            this.info = info;
            this.config = config;
            this.provider = provider;
            this.relaxInjectionRequirements = relaxInjectionRequirements;
        }
    }
}
