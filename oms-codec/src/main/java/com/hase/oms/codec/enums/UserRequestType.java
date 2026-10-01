package com.hase.oms.codec.enums;

/** Values for field {@code userRequestType} (HKEX data dictionary). */
public enum UserRequestType {
    RequestThrottleLimit(5);

    private final long code;

    UserRequestType(long code) { this.code = code; }

    public long code() { return code; }
}
