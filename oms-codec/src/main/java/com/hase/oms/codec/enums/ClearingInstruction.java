package com.hase.oms.codec.enums;

/** Values for field {@code clearingInstruction} (HKEX data dictionary). */
public enum ClearingInstruction {
    ProcessNormally(0),
    ExcludeFromAllNetting(1),
    BuyIn(14);

    private final long code;

    ClearingInstruction(long code) { this.code = code; }

    public long code() { return code; }
}
