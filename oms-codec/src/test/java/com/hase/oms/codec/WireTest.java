package com.hase.oms.codec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WireTest {

    @Test
    void littleEndianU32() {
        Wire w = Wire.allocate(4);
        w.putU32(0x01020304L);
        assertArrayEquals(new byte[]{0x04, 0x03, 0x02, 0x01}, w.array());
    }

    @Test
    void littleEndianU16() {
        Wire w = Wire.allocate(2);
        w.putU16(0x0102);
        assertArrayEquals(new byte[]{0x02, 0x01}, w.array());
    }

    @Test
    void uintRoundTrip() {
        Wire w = Wire.allocate(64);
        w.putU8(0xFF).putU16(0xFFFF).putU32(0xFFFFFFFFL).putU64(0xFFFFFFFFFFFFFFFFL);
        Wire r = Wire.wrap(w.array());
        assertEquals(0xFF, r.getU8());
        assertEquals(0xFFFF, r.getU16());
        assertEquals(0xFFFFFFFFL, r.getU32());
        assertEquals(0xFFFFFFFFFFFFFFFFL, r.getU64());
    }

    @Test
    void intRoundTrip() {
        Wire w = Wire.allocate(64);
        w.putI8(-1).putI16((short) -2).putI32(-3).putI64(-4);
        Wire r = Wire.wrap(w.array());
        assertEquals(-1, r.getI8());
        assertEquals(-2, r.getI16());
        assertEquals(-3, r.getI32());
        assertEquals(-4, r.getI64());
    }

    @Test
    void decimalRoundTrip() {
        long scaled = 1250000000L; // 12.50 × 10^8
        Wire w = Wire.allocate(8);
        w.putDecimal(scaled);
        assertEquals(scaled, Wire.wrap(w.array()).getDecimal());
    }

    @Test
    void alphaFixedNullTerminated() {
        Wire w = Wire.allocate(12);
        w.putAlphaFixed("TEST", 12);
        byte[] expected = new byte[]{'T', 'E', 'S', 'T', 0, 0, 0, 0, 0, 0, 0, 0};
        assertArrayEquals(expected, w.array());
        assertEquals("TEST", Wire.wrap(expected).getAlphaFixed(12));
    }

    @Test
    void alphaFixedTruncatesToSizeMinusOne() {
        Wire w = Wire.allocate(4);
        w.putAlphaFixed("ABCDE", 4);
        // "ABC" + null = 4 bytes; 'D','E' discarded
        assertArrayEquals(new byte[]{'A', 'B', 'C', 0}, w.array());
    }

    @Test
    void alphaVarRoundTrip() {
        Wire w = Wire.allocate(32);
        w.putAlphaVar("HELLO");
        Wire r = Wire.wrap(w.array());
        assertEquals("HELLO", r.getAlphaVar());
    }

    @Test
    void alphaVarLengthIncludesNull() {
        Wire w = Wire.allocate(32);
        w.putAlphaVar("AB");
        // length prefix = 3 (2 chars + null)
        byte[] bytes = w.array();
        assertEquals(3, bytes[0] | (bytes[1] << 8));
    }

    @Test
    void byteTypeIsAsciiChar() {
        Wire w = Wire.allocate(1);
        w.putByte('0'); // ASCII '0' = 0x30, not numeric 0
        assertEquals(0x30, w.array()[0] & 0xFF);
        assertEquals('0', Wire.wrap(w.array()).getByte());
    }
}
