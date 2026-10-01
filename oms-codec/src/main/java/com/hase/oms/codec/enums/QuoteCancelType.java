package com.hase.oms.codec.enums;

/** Values for field {@code quoteCancelType} (HKEX data dictionary). */
public enum QuoteCancelType {
    CancelForSecurities(1);

    private final long code;

    QuoteCancelType(long code) { this.code = code; }

    public long code() { return code; }
}
