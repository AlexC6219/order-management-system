package com.hase.oms.codec.enums;

/** Values for field {@code orderStatus} (HKEX data dictionary). */
public enum OrderStatus {
    New(0),
    PartiallyFilled(1),
    Filled(2),
    Cancelled(4),
    PendingCancel(6),
    Rejected(8),
    PendingNew(10),
    Expired(12),
    PendingAmend(14);

    private final long code;

    OrderStatus(long code) { this.code = code; }

    public long code() { return code; }
}
