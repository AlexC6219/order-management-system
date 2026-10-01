package com.hase.oms.codec.enums;

/** Values for field {@code orderRejectCode} (HKEX data dictionary). */
public enum OrderRejectCode {
    OrderExceedLimit(3),
    DuplicateOrder(6),
    IncorrectQty(13),
    PriceExceedsBand(16),
    ReferencePriceNotAvailable(19),
    NotionalExceedsThreshold(20),
    Other(99),
    PriceExceedsBandNoOverride(101),
    PriceExceedsBand_102(102);

    private final long code;

    OrderRejectCode(long code) { this.code = code; }

    public long code() { return code; }
}
