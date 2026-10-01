package com.hase.oms.codec.enums;

/** Values for field {@code instrumentScopeOperator} (HKEX data dictionary). */
public enum InstrumentScopeOperator {
    Include(1);

    private final long code;

    InstrumentScopeOperator(long code) { this.code = code; }

    public long code() { return code; }
}
