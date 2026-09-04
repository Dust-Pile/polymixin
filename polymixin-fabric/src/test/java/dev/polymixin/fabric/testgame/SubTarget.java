package dev.polymixin.fabric.testgame;

public interface SubTarget extends Target{
    @Override
    default String doThing() {
        return Target.super.doThing() + " Sub";
    }
}
