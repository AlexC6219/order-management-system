package com.hase.oms.codec;

import java.util.zip.CRC32C;

/**
 * CRC-32C (Castagnoli) checksum, polynomial 0x1EDC6F41 (HKEX spec §4.8).
 *
 * <p>Backed by {@link java.util.zip.CRC32C}, which implements the CRC-32C used
 * by the OCG-C protocol. The result is a 32-bit unsigned value; on the wire it
 * is encoded as a little-endian UInt32 in the message trailer.
 */
public final class Crc32c {

    private Crc32c() {}

    /** Computes CRC-32C over {@code data[offset, offset+length)}. */
    public static int compute(byte[] data, int offset, int length) {
        CRC32C crc = new CRC32C();
        crc.update(data, offset, length);
        return (int) crc.getValue();
    }

    public static int compute(byte[] data) {
        return compute(data, 0, data.length);
    }
}
