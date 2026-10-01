package com.hase.oms.codec.enums;

/** Values for field {@code entitlementType} (HKEX data dictionary). */
public enum EntitlementType {
    Trade(0),
    MakeMarket(1);

    private final long code;

    EntitlementType(long code) { this.code = code; }

    public long code() { return code; }
}
