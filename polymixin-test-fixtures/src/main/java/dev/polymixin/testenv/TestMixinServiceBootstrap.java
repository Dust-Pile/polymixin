package dev.polymixin.testenv;

import org.spongepowered.asm.service.IMixinServiceBootstrap;

public final class TestMixinServiceBootstrap implements IMixinServiceBootstrap {

    @Override
    public String getName() {
        return "PolyMixin Test Harness";
    }

    @Override
    public String getServiceClassName() {
        return "dev.polymixin.testenv.TestMixinService";
    }

    @Override
    public void bootstrap() {
    }
}
