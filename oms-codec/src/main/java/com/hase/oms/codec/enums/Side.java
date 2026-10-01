package com.hase.oms.codec.enums;

/** Values for field {@code side} (HKEX data dictionary). */
public enum Side {
    Buy(1),
    Sell(2),
    SellShort(5);

    private final long code;

    Side(long code) { this.code = code; }

    public long code() { return code; }
}
