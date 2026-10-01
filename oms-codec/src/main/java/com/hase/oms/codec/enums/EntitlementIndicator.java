package com.hase.oms.codec.enums;

/** Values for field {@code entitlementIndicator} (HKEX data dictionary). */
public enum EntitlementIndicator {
    No(0),
    Yes(1);

    private final long code;

    EntitlementIndicator(long code) { this.code = code; }

    public long code() { return code; }
}
