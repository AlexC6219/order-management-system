package com.hase.oms.codec.enums;

/** Values for field {@code massCancelRequestType} (HKEX data dictionary). */
public enum MassCancelRequestType {
    CancelForSecurity(1),
    CancelAllOrders(7),
    CancelForMarketSegment(9);

    private final long code;

    MassCancelRequestType(long code) { this.code = code; }

    public long code() { return code; }
}
