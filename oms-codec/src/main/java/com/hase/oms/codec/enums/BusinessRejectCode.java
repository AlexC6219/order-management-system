package com.hase.oms.codec.enums;

/** Values for field {@code businessRejectCode} (HKEX data dictionary). */
public enum BusinessRejectCode {
    Other(0),
    UnknownId(1),
    UnknownSecurity(2),
    UnspecifiedMessageType(3),
    ApplicationNotAvailable(4),
    ConditionallyRequiredFieldMissing(5),
    ThrottleLimitExceeded(8);

    private final long code;

    BusinessRejectCode(long code) { this.code = code; }

    public long code() { return code; }
}
