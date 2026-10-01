package com.hase.oms.codec.enums;

/** Values for field {@code massCancelRejectCode} (HKEX data dictionary). */
public enum MassCancelRejectCode {
    InvalidOrUnknownMarketSegment(8),
    Other(99);

    private final long code;

    MassCancelRejectCode(long code) { this.code = code; }

    public long code() { return code; }
}
