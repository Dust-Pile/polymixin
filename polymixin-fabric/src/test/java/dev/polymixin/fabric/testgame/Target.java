package dev.polymixin.fabric.testgame;

public interface Target {
    default String doThing() {
        return "Doing";
    }
}
