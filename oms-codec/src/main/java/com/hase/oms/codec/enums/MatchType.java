package com.hase.oms.codec.enums;

/** Values for field {@code matchType} (HKEX data dictionary). */
public enum MatchType {
    AutoMatch(4),
    CrossAuction(5);

    private final long code;

    MatchType(long code) { this.code = code; }

    public long code() { return code; }
}
