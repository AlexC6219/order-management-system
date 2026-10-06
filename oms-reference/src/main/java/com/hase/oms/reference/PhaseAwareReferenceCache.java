package com.hase.oms.reference;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-security phase-aware reference cache (DESIGN.md §5.1).
 *
 * <p>Holds the tuple {@code {spread_table, reference_price, band, vcm_state,
 * trading_phase}} per security. Trading phase is market-wide; instrument state
 * is per security. Snapshots are immutable.
 */
public final class PhaseAwareReferenceCache {

    private final Map<String, Mutable> bySecurityId = new ConcurrentHashMap<>();
    private volatile TradingPhase marketPhase = TradingPhase.CLOSED;

    public void apply(ReferenceUpdate update) {
        switch (update) {
            case ReferenceUpdate.MarketPhase p -> marketPhase = p.phase();
            case ReferenceUpdate.SecurityDefinition d ->
                    mutable(d.securityId()).spreadTableCode = d.spreadTableCode();
            case ReferenceUpdate.ReferencePrice r ->
                    mutable(r.securityId()).referencePrice = r.price();
            case ReferenceUpdate.Band b ->
                    mutable(b.securityId()).band = new PriceBand(b.lower(), b.upper());
            case ReferenceUpdate.Vcm v ->
                    mutable(v.securityId()).vcmState = v.state();
            case ReferenceUpdate.InstrumentStateChange i ->
                    mutable(i.securityId()).instrumentState = i.state();
        }
    }

    public Optional<SecurityReference> get(String securityId) {
        Mutable m = bySecurityId.get(securityId);
        if (m == null) {
            return Optional.empty();
        }
        return Optional.of(new SecurityReference(securityId, m.spreadTableCode,
                m.referencePrice, m.band, m.vcmState, marketPhase, m.instrumentState));
    }

    public TradingPhase marketPhase() {
        return marketPhase;
    }

    public int size() {
        return bySecurityId.size();
    }

    public void clear() {
        bySecurityId.clear();
    }

    private Mutable mutable(String securityId) {
        return bySecurityId.computeIfAbsent(securityId, id -> new Mutable());
    }

    private static final class Mutable {
        String spreadTableCode;
        Long referencePrice;
        PriceBand band;
        VcmState vcmState = VcmState.NONE;
        InstrumentState instrumentState = InstrumentState.NORMAL;
    }
}
