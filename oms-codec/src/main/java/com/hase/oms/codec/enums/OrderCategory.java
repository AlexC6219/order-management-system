package com.hase.oms.codec.enums;

/** Values for field {@code orderCategory} (HKEX data dictionary). */
public enum OrderCategory {
    InternalCrossOrder(1);

    private final long code;

    OrderCategory(long code) { this.code = code; }

    public long code() { return code; }
}
