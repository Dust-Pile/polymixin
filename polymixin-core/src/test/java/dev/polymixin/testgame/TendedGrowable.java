package dev.polymixin.testgame;

public class TendedGrowable implements Growable {

    @Override
    public String grow() {
        return Growable.super.grow() + "+tended";
    }
}
