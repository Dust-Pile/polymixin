package dev.polymixin.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberNameTest {

    @Test
    void extractsTheMemberNameFromAnOwnerQualifiedRefmapValue() {
        assertEquals("m_7898_", PolyMixin.memberNameOf(
                "Lnet/minecraft/world/level/block/BushBlock;m_7898_(Lnet/minecraft/core/BlockPos;)Z",
                "canSurvive"));
        assertEquals("method_9558", PolyMixin.memberNameOf(
                "Lnet/minecraft/class_2261;method_9558(Lnet/minecraft/class_2338;)Z", "canSurvive"));
    }

    @Test
    void handlesBareAndDescriptorOnlyForms() {
        assertEquals("m_7898_", PolyMixin.memberNameOf("m_7898_", "canSurvive"));
        assertEquals("m_7898_", PolyMixin.memberNameOf("m_7898_(Lnet/minecraft/core/BlockPos;)Z", "canSurvive"));
    }

    @Test
    void fallsBackWhenThereIsNothingUsable() {
        assertEquals("canSurvive", PolyMixin.memberNameOf(null, "canSurvive"));
        assertEquals("canSurvive", PolyMixin.memberNameOf("", "canSurvive"));
        assertEquals("canSurvive", PolyMixin.memberNameOf("Lnet/minecraft/Broken", "canSurvive"));
    }
}
