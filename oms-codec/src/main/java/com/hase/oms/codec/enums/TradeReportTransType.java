package com.hase.oms.codec.enums;

/** Values for field {@code tradeReportTransType} (HKEX data dictionary). */
public enum TradeReportTransType {
    New(0),
    Replace(2),
    CancelBackOut(5);

    private final long code;

    TradeReportTransType(long code) { this.code = code; }

    public long code() { return code; }
}
