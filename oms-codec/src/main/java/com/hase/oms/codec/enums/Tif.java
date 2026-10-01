package com.hase.oms.codec.enums;

/** Values for field {@code tif} (HKEX data dictionary). */
public enum Tif {
    Day(0),
    IOC(3),
    FOK(4),
    AtCrossing(9);

    private final long code;

    Tif(long code) { this.code = code; }

    public long code() { return code; }
}
