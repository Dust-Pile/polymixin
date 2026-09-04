package dev.polymixin.fabric.core;

import dev.polymixin.core.EndToEndTestHelper;
import dev.polymixin.fabric.testgame.Target;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.spongepowered.asm.mixin.Mixins;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FabricEndToEndTest {
    private static EndToEndTestHelper helper;

    @BeforeAll
    void setupE2EHelper() {
        helper = new EndToEndTestHelper(() -> Target.class, () -> {
            Mixins.addConfiguration("polymixin-lib.mixins.json");
            Mixins.addConfiguration("polymixin-fabric-test.mixins.json");
        });
    }

    @Test
    void appliesToInterfaceOnFabricMixin() throws Exception {
        helper.transformAndLoad("dev.polymixin.fabric.testgame.Target");
        helper.transformAndLoad("dev.polymixin.fabric.testgame.SubTarget");
        Class<?> base = helper.transformAndLoad("dev.polymixin.fabric.testgame.TargetImplementation");
        assertEquals("Wrap(Wrap(Wrap(Doing) Sub) Things)", helper.invoke(base, "doThing"));
    }
}
