package com.hase.oms.codec.enums;

/** Values for field {@code cancelRejectCode} (HKEX data dictionary). */
public enum CancelRejectCode {
    TooLateToCancel(0),
    UnknownOrder(1),
    PendingCancelOrReplace(3),
    DuplicateClientOrderId(6),
    Other(99);

    private final long code;

    CancelRejectCode(long code) { this.code = code; }

    public long code() { return code; }
}
