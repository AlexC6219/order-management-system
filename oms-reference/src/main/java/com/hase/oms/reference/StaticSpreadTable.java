package com.hase.oms.reference;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Loads a static HKEX-published spread table (DESIGN.md §5 fallback), so tick
 * validation survives a market-data outage.
 *
 * <p>Format: a {@code .properties} file mapping spread-table code to tick size in
 * scaled 1e8 units, e.g. {@code 01=10000000} (= 0.10).
 */
public final class StaticSpreadTable {

    private StaticSpreadTable() {}

    public static SpreadTable fromProperties(Properties properties) {
        Map<String, Long> ticks = new HashMap<>();
        for (String code : properties.stringPropertyNames()) {
            ticks.put(code, Long.parseLong(properties.getProperty(code).trim()));
        }
        return new SpreadTable(ticks);
    }

    public static SpreadTable fromProperties(InputStream in) throws IOException {
        Properties properties = new Properties();
        properties.load(in);
        return fromProperties(properties);
    }
}
