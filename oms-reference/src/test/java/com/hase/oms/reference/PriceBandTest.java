package com.hase.oms.reference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceBandTest {

    @Test
    void containsIsInclusive() {
        PriceBand band = new PriceBand(70_00000000L, 86_00000000L);
        assertTrue(band.contains(70_00000000L));
        assertTrue(band.contains(86_00000000L));
        assertTrue(band.contains(78_00000000L));
        assertFalse(band.contains(69_00000000L));
        assertFalse(band.contains(87_00000000L));
    }

    @Test
    void rejectsInvertedBand() {
        assertThrows(IllegalArgumentException.class, () -> new PriceBand(86_00000000L, 70_00000000L));
    }
}
