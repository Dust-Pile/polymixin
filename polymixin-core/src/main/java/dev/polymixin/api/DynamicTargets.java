package dev.polymixin.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface DynamicTargets {

    Class<? extends DynamicTargetProvider> value() default BypassingSubclasses.class;

    boolean relaxInjectionRequirements() default true;
}
