package com.hase.oms.reference;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticSpreadTableTest {

    @Test
    void loadsSpreadTableFromProperties() throws Exception {
        try (InputStream in = StaticSpreadTableTest.class.getResourceAsStream("/spread-table.properties")) {
            SpreadTable table = StaticSpreadTable.fromProperties(in);
            assertEquals(5, table.size());
            assertTrue(table.has("01"));
            assertEquals(10_000_000L, table.tickSize("01"));
            assertEquals(500_000_000L, table.tickSize("06"));
        }
    }

    @Test
    void unknownCodeThrows() {
        SpreadTable table = new SpreadTable(java.util.Map.of("01", 10_000_000L));
        assertThrows(IllegalArgumentException.class, () -> table.tickSize("99"));
    }
}
