package com.hase.oms.codec.enums;

/** Values for field {@code aggressorIndicator} (HKEX data dictionary). */
public enum AggressorIndicator {
    Passive(0),
    Aggressor(1);

    private final long code;

    AggressorIndicator(long code) { this.code = code; }

    public long code() { return code; }
}
