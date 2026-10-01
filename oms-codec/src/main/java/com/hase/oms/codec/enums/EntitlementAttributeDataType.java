package com.hase.oms.codec.enums;

/** Values for field {@code entitlementAttributeDataType} (HKEX data dictionary). */
public enum EntitlementAttributeDataType {
    Decimal(7);

    private final long code;

    EntitlementAttributeDataType(long code) { this.code = code; }

    public long code() { return code; }
}
