package com.hase.oms.codec.enums;

/** Values for field {@code protocolType} (HKEX data dictionary). */
public enum ProtocolType {
    Binary(1);

    private final long code;

    ProtocolType(long code) { this.code = code; }

    public long code() { return code; }
}
