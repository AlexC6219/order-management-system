package com.hase.oms.codec.enums;

/** Values for field {@code massCancelResponse} (HKEX data dictionary). */
public enum MassCancelResponse {
    Rejected(0),
    CancelForSecurity(1),
    CancelAllOrders(7),
    CancelForMarketSegment(9);

    private final long code;

    MassCancelResponse(long code) { this.code = code; }

    public long code() { return code; }
}
