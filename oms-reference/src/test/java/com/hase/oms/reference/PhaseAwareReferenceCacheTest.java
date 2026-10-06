package com.hase.oms.reference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseAwareReferenceCacheTest {

    private final PhaseAwareReferenceCache cache = new PhaseAwareReferenceCache();

    @Test
    void appliesUpdatesAndSnapshots() {
        cache.apply(new ReferenceUpdate.SecurityDefinition("5", "01"));
        cache.apply(new ReferenceUpdate.ReferencePrice("5", 78_50000000L));
        cache.apply(new ReferenceUpdate.Band("5", 70_00000000L, 86_00000000L));
        cache.apply(new ReferenceUpdate.MarketPhase(TradingPhase.CONTINUOUS));

        SecurityReference ref = cache.get("5").orElseThrow();
        assertEquals("01", ref.spreadTableCode());
        assertTrue(ref.hasReferencePrice());
        assertEquals(78_50000000L, ref.referencePrice());
        assertTrue(ref.hasBand());
        assertEquals(VcmState.NONE, ref.vcmState());
        assertEquals(TradingPhase.CONTINUOUS, ref.tradingPhase());
        assertTrue(ref.isTradable());
    }

    @Test
    void missingSecurityReturnsEmpty() {
        assertTrue(cache.get("999").isEmpty());
    }

    @Test
    void phaseChangeIsMarketWideAndReflectedInSnapshot() {
        cache.apply(new ReferenceUpdate.SecurityDefinition("5", "01"));
        cache.apply(new ReferenceUpdate.MarketPhase(TradingPhase.PRE_OPENING));
        assertEquals(TradingPhase.PRE_OPENING, cache.get("5").orElseThrow().tradingPhase());

        cache.apply(new ReferenceUpdate.MarketPhase(TradingPhase.NO_CANCELLATION));
        assertEquals(TradingPhase.NO_CANCELLATION, cache.get("5").orElseThrow().tradingPhase());
        assertFalse(cache.get("5").orElseThrow().tradingPhase().permitsAmendOrCancel());
    }

    @Test
    void vcmCoolingOffNarrowsBand() {
        cache.apply(new ReferenceUpdate.SecurityDefinition("5", "01"));
        cache.apply(new ReferenceUpdate.Band("5", 70_00000000L, 86_00000000L));
        PriceBand wide = cache.get("5").orElseThrow().band();

        cache.apply(new ReferenceUpdate.Vcm("5", VcmState.COOLING_OFF));
        cache.apply(new ReferenceUpdate.Band("5", 75_00000000L, 82_00000000L));
        SecurityReference ref = cache.get("5").orElseThrow();

        assertEquals(VcmState.COOLING_OFF, ref.vcmState());
        assertTrue(ref.band().lower() > wide.lower(), "cooling-off narrows the band");
        assertTrue(ref.band().upper() < wide.upper());
    }

    @Test
    void haltedInstrumentIsNotTradable() {
        cache.apply(new ReferenceUpdate.SecurityDefinition("5", "01"));
        cache.apply(new ReferenceUpdate.InstrumentStateChange("5", InstrumentState.HALTED));
        SecurityReference ref = cache.get("5").orElseThrow();
        assertEquals(InstrumentState.HALTED, ref.instrumentState());
        assertFalse(ref.isTradable());
    }

    @Test
    void clearEmptiesCache() {
        cache.apply(new ReferenceUpdate.SecurityDefinition("5", "01"));
        assertEquals(1, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
        assertTrue(cache.get("5").isEmpty());
    }
}
