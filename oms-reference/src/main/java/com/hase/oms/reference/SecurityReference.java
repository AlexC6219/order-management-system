package com.hase.oms.reference;

/**
 * Immutable snapshot of a security's reference state (DESIGN.md §5).
 *
 * @param referencePrice nullable (may be missing)
 * @param band           nullable (may be missing)
 */
public record SecurityReference(
        String securityId,
        String spreadTableCode,
        Long referencePrice,
        PriceBand band,
        VcmState vcmState,
        TradingPhase tradingPhase,
        InstrumentState instrumentState) {

    public boolean hasReferencePrice() {
        return referencePrice != null;
    }

    public boolean hasBand() {
        return band != null;
    }

    public boolean isTradable() {
        return instrumentState == InstrumentState.NORMAL;
    }
}
