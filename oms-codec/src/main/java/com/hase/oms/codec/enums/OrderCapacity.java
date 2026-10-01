package com.hase.oms.codec.enums;

/** Values for field {@code orderCapacity} (HKEX data dictionary). */
public enum OrderCapacity {
    Agency(1),
    Principal(2);

    private final long code;

    OrderCapacity(long code) { this.code = code; }

    public long code() { return code; }
}
