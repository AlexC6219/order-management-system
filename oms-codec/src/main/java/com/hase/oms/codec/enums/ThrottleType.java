package com.hase.oms.codec.enums;

/** Values for field {@code throttleType} (HKEX data dictionary). */
public enum ThrottleType {
    InboundRate(0);

    private final long code;

    ThrottleType(long code) { this.code = code; }

    public long code() { return code; }
}
