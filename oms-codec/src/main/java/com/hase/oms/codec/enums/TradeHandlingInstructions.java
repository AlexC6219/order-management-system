package com.hase.oms.codec.enums;

/** Values for field {@code tradeHandlingInstructions} (HKEX data dictionary). */
public enum TradeHandlingInstructions {
    TradeConfirm(0),
    TwoPartyReport(1),
    OnePartyReport(6);

    private final long code;

    TradeHandlingInstructions(long code) { this.code = code; }

    public long code() { return code; }
}
