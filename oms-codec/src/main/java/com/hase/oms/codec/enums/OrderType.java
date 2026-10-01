package com.hase.oms.codec.enums;

/** Values for field {@code orderType} (HKEX data dictionary). */
public enum OrderType {
    Market(1),
    Limit(2);

    private final long code;

    OrderType(long code) { this.code = code; }

    public long code() { return code; }
}
