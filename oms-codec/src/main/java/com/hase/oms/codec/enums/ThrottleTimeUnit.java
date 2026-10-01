package com.hase.oms.codec.enums;

/** Values for field {@code throttleTimeUnit} (HKEX data dictionary). */
public enum ThrottleTimeUnit {
    Seconds(0);

    private final long code;

    ThrottleTimeUnit(long code) { this.code = code; }

    public long code() { return code; }
}
