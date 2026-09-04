package dev.polymixin.forge.testgame;

public class TargetImplementation implements Target {
    public String doThing() {
        return Target.super.doThing() + " Things";
    }
}
