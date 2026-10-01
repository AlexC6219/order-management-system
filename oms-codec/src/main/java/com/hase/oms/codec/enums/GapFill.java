package com.hase.oms.codec.enums;

/** Values for field {@code gapFill} (HKEX data dictionary). */
public enum GapFill {
    Reset('N'),
    GapFill('Y');

    private final char code;

    GapFill(char code) { this.code = code; }

    public char code() { return code; }
}
