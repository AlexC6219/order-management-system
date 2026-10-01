package com.hase.oms.codec.enums;

/** Values for field {@code quoteStatus} (HKEX data dictionary). */
public enum QuoteStatus {
    Accepted(0),
    CancelForSymbol(1),
    Rejected(5),
    QuoteNotFound(9);

    private final long code;

    QuoteStatus(long code) { this.code = code; }

    public long code() { return code; }
}
