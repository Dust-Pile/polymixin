package dev.polymixin.fabric.core.platform;

import dev.polymixin.core.platform.Platform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PlatformTest {
    @Test
    void isFabric() {
        Assertions.assertTrue(Platform.isFabricMixin(), "Expected Fabric Mixin on Fabric");
    }
}
