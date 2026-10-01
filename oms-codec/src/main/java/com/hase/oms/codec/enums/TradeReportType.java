package com.hase.oms.codec.enums;

/** Values for field {@code tradeReportType} (HKEX data dictionary). */
public enum TradeReportType {
    New(0),
    Addendum(4),
    TradeReportCancel(6);

    private final long code;

    TradeReportType(long code) { this.code = code; }

    public long code() { return code; }
}
