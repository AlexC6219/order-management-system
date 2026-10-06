package com.hase.oms.reference;

import java.util.Map;

/**
 * HKEX spread table: maps a spread-table code (e.g. {@code "01"}) to a tick size
 * in scaled 1e8 units. Loaded from the HKEX-published file as an outage fallback
 * (DESIGN.md §5).
 */
public final class SpreadTable {

    private final Map<String, Long> tickByCode;

    public SpreadTable(Map<String, Long> tickByCode) {
        this.tickByCode = Map.copyOf(tickByCode);
    }

    public boolean has(String code) {
        return tickByCode.containsKey(code);
    }

    public long tickSize(String code) {
        Long tick = tickByCode.get(code);
        if (tick == null) {
            throw new IllegalArgumentException("Unknown spread table code: " + code);
        }
        return tick;
    }

    public int size() {
        return tickByCode.size();
    }
}
