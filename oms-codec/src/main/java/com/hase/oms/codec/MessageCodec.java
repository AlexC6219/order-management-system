package com.hase.oms.codec;

import java.util.Arrays;

/**
 * Encodes/decodes OCG-C binary messages, driven by the {@link Dictionary}.
 *
 * <p>Frame layout (HKEX spec §7.2/§7.3):
 * <pre>
 *   STX(1) Length(2) MsgType(1) SeqNum(4) PossDup(1) PossResend(1) CompId(12)
 *   BodyFieldsPresenceMap(32) [body] Checksum(4)
 * </pre>
 * The presence map is MSB-first: bit position 0 is the most-significant bit of
 * byte 0. Body fields follow in bit order. The checksum is CRC-32C over the
 * header and body (i.e. all bytes except the 4-byte trailer).
 */
public final class MessageCodec {

    private static final int STX = 0x02;
    private static final int BODY_BUFFER_CAPACITY = 8192;

    private MessageCodec() {}

    // ---- presence map (MSB-first) ----

    static void setBit(byte[] map, int pos) {
        map[pos >>> 3] |= (byte) (0x80 >>> (pos & 7));
    }

    static boolean getBit(byte[] map, int pos) {
        return (map[pos >>> 3] & (0x80 >>> (pos & 7))) != 0;
    }

    // ---- encode ----

    public static byte[] encode(Dictionary dict, Message msg) {
        Dictionary.MessageDef def = dict.message(msg.messageType());

        byte[] presenceMap = new byte[Dictionary.PRESENCE_MAP_BYTES];
        Wire body = Wire.allocate(BODY_BUFFER_CAPACITY);

        for (Dictionary.FieldEntry entry : def.fields()) {
            if (!msg.has(entry.field())) {
                continue;
            }
            Dictionary.FieldDef fd = dict.field(entry.field());
            setBit(presenceMap, entry.bit());
            encodeField(body, fd, msg.get(entry.field()));
        }

        int bodyLen = body.position();
        int total = Dictionary.HEADER_BYTES + bodyLen + Dictionary.TRAILER_BYTES;

        Wire out = Wire.allocate(total);
        out.putU8(STX);
        out.putU16(total);
        out.putU8(msg.messageType());
        out.putU32(msg.sequenceNumber());
        out.putU8(msg.possDup());
        out.putU8(msg.possResend());
        out.putAlphaFixed(msg.compId(), 12);
        out.putBytes(presenceMap, 0, Dictionary.PRESENCE_MAP_BYTES);
        out.putBytes(body.array(), 0, bodyLen);

        byte[] frame = out.array();
        int crc = Crc32c.compute(frame, 0, total - Dictionary.TRAILER_BYTES);
        out.putU32(crc);

        return frame;
    }

    private static void encodeField(Wire out, Dictionary.FieldDef fd, Object value) {
        switch (fd.type()) {
            case UINT8 -> out.putU8(((Number) value).intValue());
            case INT8 -> out.putI8(((Number) value).intValue());
            case UINT16 -> out.putU16(((Number) value).intValue());
            case INT16 -> out.putI16(((Number) value).shortValue());
            case UINT32 -> out.putU32(((Number) value).longValue());
            case INT32 -> out.putI32(((Number) value).intValue());
            case UINT64, INT64 -> out.putU64(((Number) value).longValue());
            case DECIMAL -> out.putDecimal(((Number) value).longValue());
            case BYTE -> out.putByte(((String) value).charAt(0));
            case ALPHA_FIXED -> out.putAlphaFixed((String) value, fd.size());
            case ALPHA_VAR -> out.putAlphaVar((String) value);
            case BITMAP_FIXED, BITMAP_VAR -> out.putBytes((byte[]) value);
        }
    }

    // ---- decode ----

    public static Message decode(Dictionary dict, byte[] data) {
        if (data.length < Dictionary.HEADER_BYTES + Dictionary.TRAILER_BYTES) {
            throw new IllegalArgumentException("Frame too short: " + data.length + " bytes");
        }

        Wire in = Wire.wrap(data);
        int stx = in.getU8();
        if (stx != STX) {
            throw new IllegalArgumentException("Bad STX: 0x" + Integer.toHexString(stx));
        }
        int length = in.getU16();
        int msgType = in.getU8();
        long seq = in.getU32();
        int possDup = in.getU8();
        int possResend = in.getU8();
        String compId = in.getAlphaFixed(12);
        byte[] presenceMap = in.getBytes(Dictionary.PRESENCE_MAP_BYTES);

        if (data.length < length) {
            throw new IllegalArgumentException("Declared length " + length
                    + " exceeds frame size " + data.length);
        }

        verifyChecksum(data, length);

        Dictionary.MessageDef def = dict.message(msgType);

        Message msg = new Message(msgType)
                .sequenceNumber(seq)
                .possDup(possDup)
                .possResend(possResend)
                .compId(compId);

        for (Dictionary.FieldEntry entry : def.fields()) {
            if (getBit(presenceMap, entry.bit())) {
                Dictionary.FieldDef fd = dict.field(entry.field());
                msg.put(entry.field(), decodeField(in, fd));
            }
        }

        return msg;
    }

    private static Object decodeField(Wire in, Dictionary.FieldDef fd) {
        return switch (fd.type()) {
            case UINT8 -> (long) in.getU8();
            case INT8 -> (long) in.getI8();
            case UINT16 -> (long) in.getU16();
            case INT16 -> (long) in.getI16();
            case UINT32 -> in.getU32();
            case INT32 -> (long) in.getI32();
            case UINT64, INT64 -> in.getU64();
            case DECIMAL -> in.getDecimal();
            case BYTE -> String.valueOf(in.getByte());
            case ALPHA_FIXED -> in.getAlphaFixed(fd.size());
            case ALPHA_VAR -> in.getAlphaVar();
            case BITMAP_FIXED, BITMAP_VAR -> in.getBytes(fd.size());
        };
    }

    private static void verifyChecksum(byte[] data, int length) {
        int crc = Crc32c.compute(data, 0, length - Dictionary.TRAILER_BYTES);
        int stored = readTrailingU32(data, length - Dictionary.TRAILER_BYTES);
        if (crc != stored) {
            throw new IllegalArgumentException("Checksum mismatch: computed 0x"
                    + Integer.toHexString(crc) + " vs stored 0x" + Integer.toHexString(stored));
        }
    }

    private static int readTrailingU32(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }

    /** Reference implementation used by golden-vector tests (same as encode). */
    static byte[] encodeHeaderBodyOnly(Dictionary dict, Message msg) {
        byte[] frame = encode(dict, msg);
        return Arrays.copyOf(frame, frame.length - Dictionary.TRAILER_BYTES);
    }
}
