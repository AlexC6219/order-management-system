package com.hase.oms.codec.enums;

/** Values for field {@code testMessageIndicator} (HKEX data dictionary). */
public enum TestMessageIndicator {
    ProductionMode(0),
    TestMode(1);

    private final long code;

    TestMessageIndicator(long code) { this.code = code; }

    public long code() { return code; }
}
