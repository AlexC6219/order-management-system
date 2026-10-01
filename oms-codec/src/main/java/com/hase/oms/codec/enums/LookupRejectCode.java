package com.hase.oms.codec.enums;

/** Values for field {@code lookupRejectCode} (HKEX data dictionary). */
public enum LookupRejectCode {
    InvalidClient(0),
    InvalidServiceType(1),
    InvalidProtocol(2),
    ClientBlocked(3),
    Other(4);

    private final long code;

    LookupRejectCode(long code) { this.code = code; }

    public long code() { return code; }
}
