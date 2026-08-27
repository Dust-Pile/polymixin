package dev.polymixin.testgame;

public class CropBlockA extends BaseBlock {

    @Override
    public boolean canSurvive() {
        return false;
    }

    @Override
    public String describe() {
        return "cropA";
    }

    @Override
    public String label() {
        return "LA";
    }

    @Override
    public String tag() {
        return "TA";
    }

    @Override
    public String mark() {
        return "MA";
    }

    @Override
    public String badge() {
        return "BA";
    }

    @Override
    public String chain() {
        return super.chain() + "+A";
    }

    @Override
    public int effectiveHardness() {
        return this.computeHardness() * 2;
    }
}
