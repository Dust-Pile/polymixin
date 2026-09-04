package dev.polymixin.forge.core.platform;

import dev.polymixin.core.platform.Platform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PlatformTest {
    @Test
    void isFabric() {
        Assertions.assertFalse(Platform.isFabricMixin(), "Expected Non Fabric Mixin on Forge");
    }
}
