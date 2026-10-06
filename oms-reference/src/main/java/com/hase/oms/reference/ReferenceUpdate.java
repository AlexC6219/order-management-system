package com.hase.oms.reference;

/**
 * A reference-data update published by a {@link ReferenceDataSource} and applied
 * to the {@link PhaseAwareReferenceCache}.
 *
 * <p>Mirrors the OMD-C messages in DESIGN.md §5: Security Definition (spread
 * table), Reference Price, VCM Trigger (band), Closing/Nominal price, Trading
 * Session Status (market phase), Security Status (instrument state).
 */
public sealed interface ReferenceUpdate {

    record SecurityDefinition(String securityId, String spreadTableCode) implements ReferenceUpdate {}

    record ReferencePrice(String securityId, long price) implements ReferenceUpdate {}

    record Band(String securityId, long lower, long upper) implements ReferenceUpdate {}

    record Vcm(String securityId, VcmState state) implements ReferenceUpdate {}

    record InstrumentStateChange(String securityId, InstrumentState state) implements ReferenceUpdate {}

    /** Market-wide trading phase (Trading Session Status). */
    record MarketPhase(TradingPhase phase) implements ReferenceUpdate {}
}
