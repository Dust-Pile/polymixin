package dev.polymixin.fabric.testgame;

public class TargetImplementation implements SubTarget {
    public String doThing() {
        return SubTarget.super.doThing() + " Things";
    }
}
