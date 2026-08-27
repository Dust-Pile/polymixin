package dev.polymixin.api;

import java.util.Set;

public interface TargetInspector {

    boolean declaresMethod(String className, String methodName);

    boolean delegatesToSuper(String className, String methodName);

    /**
     * Maps a method name written the way an injector's {@code method =} is written into the name it
     * has at runtime, using the owning config's refmap. Returns the input unchanged when there is
     * no mapping, which is the correct answer in a development environment.
     */
    String resolveMethodName(String methodName);

    /** Whether the class declares any override that does not call its super implementation. */
    boolean bypassesAnyOverride(String className);

    /** Whether the mixin injects into a constructor, where super-delegation reasoning does not apply. */
    boolean mixinTargetsConstructor();

    /**
     * The method names this mixin's injectors target, already mapped to runtime names, or
     * {@code null} when they cannot be read confidently.
     */
    Set<String> injectedMethodNames();
}
