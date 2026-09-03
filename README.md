# PolyMixin

PolyMixin is a Mixin library that extends a mixin's target list at runtime. Put `@DynamicTargets` on
a mixin and it gets applied to every subclass of the class it targets, including subclasses that ship
in somebody else's mod, without you naming any of them. Further target modification is configurable.

Supports Forge 1.20.1, NeoForge 1.21.1 and Fabric 1.21.1.

## Why

Mixin applies to the classes you name and to nothing else. That is usually what you want, right up
until the method you patched gets overridden. Here's an example:

```java
@Mixin(BushBlock.class)
public abstract class BushBlockMixin {

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true, require = 1)
    private void allowOnSlabs(BlockState state, LevelReader level, BlockPos pos,
                              CallbackInfoReturnable<Boolean> cir) {
        // let plants sit on bottom slabs
    }
}
```

Vanilla 1.20.1 has 29 subclasses of `BushBlock`. Eighteen of them override `canSurvive` or
`mayPlaceOn` in a way that can skip the parent entirely, so the patch above does nothing for crops,
mushrooms, sea pickles, lily pads, nether wart, or anything a mod adds later. The usual answers are:

- list every subclass in `@Mixin(targets = ...)` and ship a new build every time somebody reports
  another broken plant, or
- write an `IMixinConfigPlugin` that builds the list at runtime, which means bringing your own class
  scanner, dealing with SRG, intermediary and Mojang member names, and finding a point in Mixin's
  startup where new targets are still accepted.

PolyMixin is a new, simple solution.

## Setup

The artifacts are `polymixin-fabric`, `polymixin-neoforge` and `polymixin-forge` under the group
`dev.polymixin`. Each one is a complete mod jar containing the core, the platform code and
ClassGraph. Bundle one into your mod with jar-in-jar. If several mods ship it, the loader keeps the
newest and drops the rest, so there is nothing to relocate and nothing to coordinate.

Add the repository and gradle dependency through curse maven. Project page: https://www.curseforge.com/minecraft/mc-mods/polymixin

### Mod metadata

Declaring the dependency is optional. PolyMixin finds your mixin either way. Add it anyway so the
loader starts PolyMixin before your mod, and so users get a sensible message instead of a silently
broken patch if the bundled copy ever goes missing.

`fabric.mod.json`:

```json
"depends": {
  "polymixin": "*"
}
```

`mods.toml` or `neoforge.mods.toml`:

```toml
[[dependencies.yourmod]]
modId = "polymixin"
mandatory = true
versionRange = "[1.0,)"
ordering = "AFTER"
side = "BOTH"
```

## Usage

### The annotation

Annotate the mixin. Nothing else changes, and your `mixins.json` stays exactly as it is.

```java
@Mixin(BushBlock.class)
@DynamicTargets
public abstract class BushBlockMixin {

    @Shadow
    protected abstract boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos);

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true, require = 1)
    private void yourmod$allowOnSlabs(BlockState state, LevelReader level, BlockPos pos,
                                      CallbackInfoReturnable<Boolean> cir) {
        // your patch, now running on the subclasses that would have skipped it
    }
}
```

`@DynamicTargets` takes one more option, `relaxInjectionRequirements`, which defaults to `true`. It
rewrites `require` to `0` on the generated copies only. Turn it off if you would rather have a
subclass that stops matching your injector crash loudly.

### An existing config plugin

If you already have an `IMixinConfigPlugin`, make it implement `DynamicTargetProvider` as well. Every
mixin in that config then gets dynamic targets and you do not need the annotation.

```java
public class MyPlugin implements IMixinConfigPlugin, DynamicTargetProvider {

    @Override
    public Collection<ClassInfo> provideTargets(TargetContext ctx) {
        return ctx.subclassesThatBypass();
    }

    // the rest of your plugin, unchanged
}
```

### Picking targets yourself

Point the annotation at a provider class. It needs a public no-arg constructor and has to live
outside your mixin package, because everything inside that package is handed to Mixin as a mixin.

```java
@Mixin(BushBlock.class)
@DynamicTargets(EverySubclass.class)
public abstract class BushBlockMixin { }
```

```java
public final class EverySubclass implements DynamicTargetProvider {

    @Override
    public Collection<ClassInfo> provideTargets(TargetContext ctx) {
        return ctx.subclassesOfDeclaredTargets();
    }
}
```

`TargetContext` is what the provider has to work with:

| method | returns |
|---|---|
| `declaredTargets()` | the classes your `@Mixin` listed, already mapped |
| `subclassesOfDeclaredTargets()` | every subclass, no filtering |
| `implementersOfDeclaredTargets()` | for interface targets, every implementing class and extending interface |
| `subclassesThatBypass()` | the default, see below |
| `subclassesOverriding("foo")` | subclasses that declare `foo` themselves |
| `subclassesBypassing("foo")` | declare `foo` and do not always call `super.foo()` |
| `declaresMethod(classInfo, "foo")` | single class check |
| `delegatesToSuper(classInfo, "foo")` | single class check |
| `scanResult()` | the raw ClassGraph `ScanResult` |
| `configName()`, `mixinClassName()` | who is asking |

Write method names the way you write them in `method =` on your injectors. They go through your
refmap, so `canSurvive` resolves in dev and in an obfuscated build alike.

## Which subclasses get targeted

The default provider reads the `method =` selectors off your own injectors and keeps only the
subclasses that can actually bypass them. A subclass is dropped when it either

- does not declare the method at all, since it already inherits your patched version, or
- overrides it but always calls `super`, since your patched version still runs in the parent frame.

Always means always:

```java
// dropped, super runs on every path
return super.mayPlaceOn(state, level, pos) || state.is(Blocks.NETHERRACK);

// kept, this returns early on clay and never reaches the patch
return state.is(Blocks.CLAY) || super.mayPlaceOn(state, level, pos);
```

If anything can jump over the super call, the subclass is targeted. On vanilla 1.20.1 `BushBlock`
that takes 31 candidates down to 19, the same 19 on all three loaders.

PolyMixin falls back to targeting every subclass when it cannot read your selectors with confidence.
That happens with wildcards (`method = "get*"`), quantifiers, injectors that only use `target =`, and
anything injecting into `<init>`, where the super call reasoning does not apply because every
constructor calls one.

## Interface targets

`@Mixin` can name an interface. PolyMixin then reads its implementers instead of its subclasses:

```java
@Mixin(Growable.class)
@DynamicTargets
public interface GrowableMixin {

    @Inject(method = "grow", at = @At("HEAD"), cancellable = true, require = 1)
    default void yourmod$grow(CallbackInfoReturnable<String> cir) {
        // runs on every class implementing Growable that declares grow() itself
    }
}
```

The mixin has to be an interface, because Mixin rejects a class mixin whose target is one. It also
needs `"compatibilityLevel": "JAVA_8"` or higher in your `mixins.json`. Because the source is an
interface and the generated targets are classes, the copies are emitted as abstract class mixins.

Two things work differently from a class target:

- **Mixin cannot apply an injector to an interface**, so the declared target itself is never patched.
  PolyMixin detaches the mixin from it rather than leaving a failed apply in the log. An implementer
  that inherits a default method without declaring an override therefore cannot be patched at all -
  there is no method on it for the injector to match.
- **Delegating to `Growable.super.grow()` is not a reason to skip an implementer**, since the
  interface default it delegates to is unpatched. Every implementer that declares the method gets a
  copy, and the double-fire caveat above does not apply.

Accessor and invoker interface mixins are untouched: implementers already inherit them, so nothing
is generated.

## Behaviour worth knowing

**An injector can fire twice.** If a subclass calls `super` on some paths and not others, like
`AzaleaBlock.mayPlaceOn`, the copy on the subclass and the original on the parent both run on the
paths that do reach `super`. Losing the patch is worse than running it twice, so those subclasses are
kept. Write injectors that do not care how often they run and this never comes up. Return
`subclassesOfDeclaredTargets()` from your own provider and you get it on every delegating subclass.


```gradle
annotationProcessor 'io.github.llamalad7:mixinextras-common:0.4.1'
```

Half a second on a normal setup, about a second on a big pack. If no loaded mod uses PolyMixin,
nothing is scanned at all.

## System properties

| property | default | effect |
|---|---|---|
| `polymixin.disable` | `false` | turn the whole library off |
| `polymixin.debug` | `false` | log classpath roots, every generated mixin and every apply |
| `polymixin.strict` | `false` | fail at startup if PolyMixin cannot hook into Mixin, instead of carrying on degraded |
| `polymixin.discovery` | `auto` | `dependents` only inspects mods that declare a dependency on PolyMixin, `plugins` ignores `@DynamicTargets` entirely |
| `polymixin.scan.threads` | `1` | faster scan, small deadlock risk, see RESEARCH.md |
| `polymixin.scan.rejectPackages` | none | comma separated packages to skip while scanning |
| `polymixin.scan.noExtractNested` | `false` | stop copying jar-in-jar files to temp, at the cost of not seeing the classes inside them |
| `polymixin.audit.disable` | `false` | skip the startup warning about injectors with no explicit `require` |
| `polymixin.summary.delaySeconds` | `30` | how long after the last apply the summary is printed |

