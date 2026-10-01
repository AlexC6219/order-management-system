package com.hase.oms.codec.enums;

/** Values for field {@code tradeReportStatus} (HKEX data dictionary). */
public enum TradeReportStatus {
    Accepted(0),
    Rejected(1);

    private final long code;

    TradeReportStatus(long code) { this.code = code; }

    public long code() { return code; }
}
