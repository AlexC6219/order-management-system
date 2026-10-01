package com.hase.oms.codec.enums;

/** Values for field {@code amendRejectCode} (HKEX data dictionary). */
public enum AmendRejectCode {
    TooLateToAmend(0),
    UnknownOrder(1),
    PendingCancelOrReplace(3),
    DuplicateClientOrderId(6),
    PriceExceedsBand(8),
    Other(99),
    ReferencePriceNotAvailable(100),
    PriceExceedsBandNoOverride(101),
    PriceExceedsBand_102(102),
    NotionalExceedsThreshold(103);

    private final long code;

    AmendRejectCode(long code) { this.code = code; }

    public long code() { return code; }
}
