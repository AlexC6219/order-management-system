package com.hase.oms.codec.enums;

/** Values for field {@code positionEffect} (HKEX data dictionary). */
public enum PositionEffect {
    Close(1);

    private final long code;

    PositionEffect(long code) { this.code = code; }

    public long code() { return code; }
}
