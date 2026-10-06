package com.hase.oms.reference;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScriptedReferenceDataSourceTest {

    @Test
    void publishesUpdatesIntoCache() {
        PhaseAwareReferenceCache cache = new PhaseAwareReferenceCache();
        ScriptedReferenceDataSource source = new ScriptedReferenceDataSource(List.of(
                new ReferenceUpdate.SecurityDefinition("5", "01"),
                new ReferenceUpdate.ReferencePrice("5", 78_50000000L),
                new ReferenceUpdate.MarketPhase(TradingPhase.CONTINUOUS)));

        source.subscribe(cache::apply);
        source.start();
        source.publishAll();

        SecurityReference ref = cache.get("5").orElseThrow();
        assertEquals("01", ref.spreadTableCode());
        assertEquals(TradingPhase.CONTINUOUS, ref.tradingPhase());
        assertTrue(ref.hasReferencePrice());
    }

    @Test
    void publishBeforeStartFails() {
        ScriptedReferenceDataSource source = new ScriptedReferenceDataSource(List.of());
        source.subscribe(u -> {});
        assertThrows(IllegalStateException.class, source::publishAll);
    }
}
