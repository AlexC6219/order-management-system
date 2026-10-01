package com.hase.oms.codec;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Crc32cTest {

    @Test
    void knownCheckValue() {
        // CRC-32C (Castagnoli) of ASCII "123456789" — the standard check value.
        byte[] data = "123456789".getBytes(StandardCharsets.US_ASCII);
        assertEquals(0xE3069283, Crc32c.compute(data));
    }

    @Test
    void emptyInput() {
        assertEquals(0, Crc32c.compute(new byte[0]));
    }

    @Test
    void offsetAndLength() {
        byte[] data = "123456789".getBytes(StandardCharsets.US_ASCII);
        assertEquals(0xE3069283, Crc32c.compute(data, 0, data.length));
    }
}
