package dev.polymixin.forge.testgame;

public interface Target {
    default String doThing() {
        return "Doing";
    }
}
