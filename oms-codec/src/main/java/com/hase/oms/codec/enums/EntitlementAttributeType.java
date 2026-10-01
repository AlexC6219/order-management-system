package com.hase.oms.codec.enums;

/** Values for field {@code entitlementAttributeType} (HKEX data dictionary). */
public enum EntitlementAttributeType {
    MinimumVolumeObligation(4000),
    MaximumSpreadObligation(4001);

    private final long code;

    EntitlementAttributeType(long code) { this.code = code; }

    public long code() { return code; }
}
