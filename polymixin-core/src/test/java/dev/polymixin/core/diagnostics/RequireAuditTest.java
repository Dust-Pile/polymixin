package dev.polymixin.core.diagnostics;

import dev.polymixin.testenv.TestMixinService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequireAuditTest {

    private static ClassNode read(String name) throws Exception {
        ClassNode node = new ClassNode();
        new ClassReader(TestMixinService.readBytes(name)).accept(node, 0);
        return node;
    }

    @Test
    void flagsInjectorsWithoutExplicitRequire() throws Exception {
        List<String> findings = RequireAudit.findings("dev.polymixin.testmixins.MixinNoRequire",
                read("dev.polymixin.testmixins.MixinNoRequire"));

        assertEquals(1, findings.size(), findings.toString());
        assertTrue(findings.get(0).contains("polymixin$noRequire"), findings.get(0));
        assertTrue(findings.get(0).contains("@Inject"), findings.get(0));
    }

    @Test
    void staysQuietWhenRequireIsExplicit() throws Exception {
        assertEquals(List.of(), RequireAudit.findings("dev.polymixin.testmixins.MixinBaseBlock",
                read("dev.polymixin.testmixins.MixinBaseBlock")));
    }
}
