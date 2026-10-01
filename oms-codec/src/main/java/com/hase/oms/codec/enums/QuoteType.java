package com.hase.oms.codec.enums;

/** Values for field {@code quoteType} (HKEX data dictionary). */
public enum QuoteType {
    Tradable(1);

    private final long code;

    QuoteType(long code) { this.code = code; }

    public long code() { return code; }
}
