package dev.polymixin.testgame;

public class BaseBlock {

    protected int hardness = 3;

    public boolean canSurvive() {
        return false;
    }

    public String describe() {
        return "base";
    }

    public String label() {
        return "L";
    }

    public String tag() {
        return "T";
    }

    public String mark() {
        return "M";
    }

    public String badge() {
        return "B";
    }

    public String chain() {
        return "base";
    }

    public String never() {
        return "N";
    }

    protected int computeHardness() {
        return this.hardness;
    }

    public int effectiveHardness() {
        return this.computeHardness();
    }
}
