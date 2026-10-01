package com.hase.oms.codec.enums;

/** Values for field {@code tradeReportRejectCode} (HKEX data dictionary). */
public enum TradeReportRejectCode {
    InvalidTradeType(4),
    PriceExceedsBand(5),
    ReferencePriceNotAvailable(6),
    NotionalExceedsThreshold(7),
    Other(99);

    private final long code;

    TradeReportRejectCode(long code) { this.code = code; }

    public long code() { return code; }
}
