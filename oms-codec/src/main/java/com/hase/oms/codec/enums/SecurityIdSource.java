package com.hase.oms.codec.enums;

/** Values for field {@code securityIdSource} (HKEX data dictionary). */
public enum SecurityIdSource {
    ExchangeSymbol(8);

    private final long code;

    SecurityIdSource(long code) { this.code = code; }

    public long code() { return code; }
}
