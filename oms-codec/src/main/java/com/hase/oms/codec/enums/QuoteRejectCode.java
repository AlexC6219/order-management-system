package com.hase.oms.codec.enums;

/** Values for field {@code quoteRejectCode} (HKEX data dictionary). */
public enum QuoteRejectCode {
    InvalidPrice(8),
    PriceExceedsBand(10),
    NotionalExceedsThreshold(14),
    ReferencePriceNotAvailable(16),
    Other(99),
    PriceExceedsBandNoOverride(101),
    PriceExceedsBand_102(102);

    private final long code;

    QuoteRejectCode(long code) { this.code = code; }

    public long code() { return code; }
}
