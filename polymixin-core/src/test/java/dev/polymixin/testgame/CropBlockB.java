package dev.polymixin.testgame;

public class CropBlockB extends BaseBlock {

    @Override
    public boolean canSurvive() {
        return false;
    }

    @Override
    public String tag() {
        return "TB";
    }

    @Override
    public int effectiveHardness() {
        return this.computeHardness() + 1;
    }
}
