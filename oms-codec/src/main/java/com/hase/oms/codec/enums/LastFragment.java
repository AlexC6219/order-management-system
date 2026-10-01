package com.hase.oms.codec.enums;

/** Values for field {@code lastFragment} (HKEX data dictionary). */
public enum LastFragment {
    No(0),
    Yes(1);

    private final long code;

    LastFragment(long code) { this.code = code; }

    public long code() { return code; }
}
