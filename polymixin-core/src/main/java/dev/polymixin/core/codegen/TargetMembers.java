package dev.polymixin.core.codegen;

public interface TargetMembers {

    boolean declaresMethodNamed(String name);

    boolean declaresMethod(String name, String desc);

    boolean declaresField(String name, String desc);

    boolean inheritsAccessibleMethod(String name, String desc);

    boolean inheritsAccessibleField(String name, String desc);
}
