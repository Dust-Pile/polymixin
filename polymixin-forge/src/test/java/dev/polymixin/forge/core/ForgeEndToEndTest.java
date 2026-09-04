package dev.polymixin.forge.core;

import dev.polymixin.core.EndToEndTestHelper;
import dev.polymixin.forge.testgame.Target;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.spongepowered.asm.mixin.Mixins;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ForgeEndToEndTest {
    private static EndToEndTestHelper helper;

    @BeforeAll
    void setupE2EHelper() {
        helper = new EndToEndTestHelper(() -> Target.class, () -> {
            Mixins.addConfiguration("polymixin-lib.mixins.json");
            Mixins.addConfiguration("polymixin-forge-test.mixins.json");
        });
    }

    @Test
    void appliesToInterfaceOnFabricMixin() throws Exception {
        helper.transformAndLoad("dev.polymixin.forge.testgame.Target");
        Class<?> base = helper.transformAndLoad("dev.polymixin.forge.testgame.TargetImplementation");
        assertEquals("Wrap(Doing Things)", helper.invoke(base, "doThing"));
    }
}
