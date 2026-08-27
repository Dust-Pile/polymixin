package dev.polymixin.testgame;

public class ConditionalSuperBlock extends BaseBlock {

    public static volatile boolean shortCircuit;

    @Override
    public String chain() {
        if (shortCircuit) {
            return "short";
        }
        return super.chain();
    }
}
