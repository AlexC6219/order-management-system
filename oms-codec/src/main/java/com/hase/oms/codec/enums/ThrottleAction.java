package com.hase.oms.codec.enums;

/** Values for field {@code throttleAction} (HKEX data dictionary). */
public enum ThrottleAction {
    Rejected(2);

    private final long code;

    ThrottleAction(long code) { this.code = code; }

    public long code() { return code; }
}
