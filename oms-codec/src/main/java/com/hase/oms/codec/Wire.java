package com.hase.oms.codec;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Little-endian byte buffer for OCG-C binary encoding/decoding.
 *
 * <p>All OCG-C integers are little-endian (HKEX spec §6.1); the only exception
 * is the RSA ciphertext inside the Logon password, which is big-endian before
 * base-64 encoding (handled outside this class).
 */
public final class Wire {

    private final ByteBuffer buf;

    private Wire(ByteBuffer buf) {
        this.buf = buf.order(ByteOrder.LITTLE_ENDIAN);
    }

    public static Wire allocate(int capacity) {
        return new Wire(ByteBuffer.allocate(capacity));
    }

    public static Wire wrap(byte[] data) {
        return new Wire(ByteBuffer.wrap(data));
    }

    // ---- writing (little-endian) ----

    public Wire putU8(int v) {
        buf.put((byte) v);
        return this;
    }

    public Wire putU16(int v) {
        buf.putShort((short) v);
        return this;
    }

    public Wire putU32(long v) {
        buf.putInt((int) v);
        return this;
    }

    public Wire putU64(long v) {
        buf.putLong(v);
        return this;
    }

    public Wire putI8(int v) {
        buf.put((byte) v);
        return this;
    }

    public Wire putI16(short v) {
        buf.putShort(v);
        return this;
    }

    public Wire putI32(int v) {
        buf.putInt(v);
        return this;
    }

    public Wire putI64(long v) {
        buf.putLong(v);
        return this;
    }

    public Wire putDecimal(long scaled) {
        buf.putLong(scaled);
        return this;
    }

    /** Writes a single ASCII character (OCG-C {@code byte} data type). */
    public Wire putByte(char c) {
        buf.put((byte) c);
        return this;
    }

    public Wire putBytes(byte[] b) {
        buf.put(b);
        return this;
    }

    public Wire putBytes(byte[] b, int off, int len) {
        buf.put(b, off, len);
        return this;
    }

    /** Null-terminated fixed-length ASCII; {@code size} includes the null. */
    public Wire putAlphaFixed(String s, int size) {
        byte[] bytes = s.getBytes(StandardCharsets.US_ASCII);
        int len = Math.min(bytes.length, size - 1);
        buf.put(bytes, 0, len);
        for (int i = len; i < size; i++) {
            buf.put((byte) 0);
        }
        return this;
    }

    /** Variable-length ASCII: 2-byte UInt16 length (includes null) + bytes. */
    public Wire putAlphaVar(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.US_ASCII);
        buf.putShort((short) (bytes.length + 1));
        buf.put(bytes);
        buf.put((byte) 0);
        return this;
    }

    public Wire putZero(int count) {
        for (int i = 0; i < count; i++) {
            buf.put((byte) 0);
        }
        return this;
    }

    // ---- reading (little-endian) ----

    public int getU8() {
        return buf.get() & 0xFF;
    }

    public int getU16() {
        return buf.getShort() & 0xFFFF;
    }

    public long getU32() {
        return buf.getInt() & 0xFFFFFFFFL;
    }

    public long getU64() {
        return buf.getLong();
    }

    public int getI8() {
        return buf.get();
    }

    public short getI16() {
        return buf.getShort();
    }

    public int getI32() {
        return buf.getInt();
    }

    public long getI64() {
        return buf.getLong();
    }

    public long getDecimal() {
        return buf.getLong();
    }

    public char getByte() {
        return (char) (buf.get() & 0xFF);
    }

    public byte[] getBytes(int n) {
        byte[] out = new byte[n];
        buf.get(out);
        return out;
    }

    /** Reads a null-terminated fixed-length ASCII field. */
    public String getAlphaFixed(int size) {
        byte[] tmp = new byte[size];
        buf.get(tmp);
        int len = 0;
        while (len < size && tmp[len] != 0) {
            len++;
        }
        return new String(tmp, 0, len, StandardCharsets.US_ASCII);
    }

    /** Reads a variable-length ASCII field (2-byte length prefix). */
    public String getAlphaVar() {
        int len = getU16();
        byte[] tmp = new byte[len];
        buf.get(tmp);
        int end = 0;
        while (end < len && tmp[end] != 0) {
            end++;
        }
        return new String(tmp, 0, end, StandardCharsets.US_ASCII);
    }

    public void skip(int n) {
        buf.position(buf.position() + n);
    }

    // ---- position / limits ----

    public int position() {
        return buf.position();
    }

    public void position(int p) {
        buf.position(p);
    }

    public int remaining() {
        return buf.remaining();
    }

    public byte[] array() {
        return buf.array();
    }

    public byte[] toByteArray() {
        int pos = buf.position();
        byte[] out = new byte[pos];
        buf.flip();
        buf.get(out);
        return out;
    }

    public ByteBuffer raw() {
        return buf;
    }
}
