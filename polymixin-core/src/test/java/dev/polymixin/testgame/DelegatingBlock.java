package dev.polymixin.testgame;

public class DelegatingBlock extends BaseBlock {

    @Override
    public String chain() {
        return super.chain() + "+delegating";
    }

    @Override
    public String tag() {
        return "TD";
    }
}
