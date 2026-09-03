package dev.polymixin.testgame;

public interface Growable {

    default String grow() {
        return "grow";
    }
}
