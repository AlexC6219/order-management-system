package com.hase.oms.codec.enums;

/** Values for field {@code execType} (HKEX data dictionary). */
public enum ExecType {
    New('0'),
    Cancel('4'),
    Amend('5'),
    Reject('8'),
    Expire('C'),
    Trade('F'),
    TradeCancel('H'),
    System('L'),
    CancelReject('X'),
    AmendReject('Y');

    private final char code;

    ExecType(char code) { this.code = code; }

    public char code() { return code; }
}
