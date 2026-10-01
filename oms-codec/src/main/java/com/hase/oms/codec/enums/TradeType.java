package com.hase.oms.codec.enums;

/** Values for field {@code tradeType} (HKEX data dictionary). */
public enum TradeType {
    LateTrade(4),
    PrivatelyNegotiated(22),
    OddLotTrade(102),
    OverseasTrade(104);

    private final long code;

    TradeType(long code) { this.code = code; }

    public long code() { return code; }
}
