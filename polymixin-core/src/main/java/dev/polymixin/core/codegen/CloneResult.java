package dev.polymixin.core.codegen;

import java.util.List;

public final class CloneResult {

    private final byte[] bytes;
    private final String skipReason;
    private final List<String> strippedShadows;

    private CloneResult(byte[] bytes, String skipReason, List<String> strippedShadows) {
        this.bytes = bytes;
        this.skipReason = skipReason;
        this.strippedShadows = strippedShadows;
    }

    static CloneResult generated(byte[] bytes, List<String> strippedShadows) {
        return new CloneResult(bytes, null, strippedShadows);
    }

    static CloneResult skipped(String reason) {
        return new CloneResult(null, reason, List.of());
    }

    public byte[] bytes() {
        return this.bytes;
    }

    public String skipReason() {
        return this.skipReason;
    }

    public List<String> strippedShadows() {
        return this.strippedShadows;
    }

    public boolean isSkipped() {
        return this.skipReason != null;
    }
}
