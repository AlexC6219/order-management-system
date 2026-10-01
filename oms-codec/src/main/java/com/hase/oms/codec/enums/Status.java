package com.hase.oms.codec.enums;

/** Values for field {@code status} (HKEX data dictionary). */
public enum Status {
    Accepted(0),
    Rejected(1);

    private final long code;

    Status(long code) { this.code = code; }

    public long code() { return code; }
}
