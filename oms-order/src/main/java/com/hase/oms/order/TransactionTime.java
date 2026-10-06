package com.hase.oms.order;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * OCG-C {@code Transaction Time} format: {@code YYYYMMDD-HH:MM:SS.ssssss}, UTC,
 * alphanumeric fixed 25 (DESIGN.md §6.3).
 */
public final class TransactionTime {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSSSSS").withZone(ZoneOffset.UTC);

    private TransactionTime() {}

    public static String now(Clock clock) {
        return FORMAT.format(clock.instant());
    }
}
