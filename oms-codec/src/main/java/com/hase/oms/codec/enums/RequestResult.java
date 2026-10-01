package com.hase.oms.codec.enums;

/** Values for field {@code requestResult} (HKEX data dictionary). */
public enum RequestResult {
    ValidRequest(0),
    InvalidOrUnsupported(1),
    NoDataFound(2),
    NotAuthorized(3),
    DataTemporarilyUnavailable(4),
    RequestNotSupported(5),
    Other(99);

    private final long code;

    RequestResult(long code) { this.code = code; }

    public long code() { return code; }
}
