package com.hase.oms.codec.enums;

/** Values for field {@code lotType} (HKEX data dictionary). */
public enum LotType {
    OddLot(1),
    RoundLot(2);

    private final long code;

    LotType(long code) { this.code = code; }

    public long code() { return code; }
}
