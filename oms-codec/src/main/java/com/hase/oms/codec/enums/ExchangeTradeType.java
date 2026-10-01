package com.hase.oms.codec.enums;

/** Values for field {@code exchangeTradeType} (HKEX data dictionary). */
public enum ExchangeTradeType {
    Manual('M'),
    ManualNonStandardPrice('S'),
    SpecialLot('Q'),
    OddLot('P'),
    PreviousDay('R'),
    Overseas('V'),
    SpecialLotSemiAuto('E'),
    OddLotSemiAuto('O');

    private final char code;

    ExchangeTradeType(char code) { this.code = code; }

    public char code() { return code; }
}
