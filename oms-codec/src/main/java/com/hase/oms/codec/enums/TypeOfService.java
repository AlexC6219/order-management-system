package com.hase.oms.codec.enums;

/** Values for field {@code typeOfService} (HKEX data dictionary). */
public enum TypeOfService {
    OrderInput(1);

    private final long code;

    TypeOfService(long code) { this.code = code; }

    public long code() { return code; }
}
