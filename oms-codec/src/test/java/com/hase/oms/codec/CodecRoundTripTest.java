package com.hase.oms.codec;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Property-style round-trip: every one of the 27 message types, with every
 * field populated, must satisfy encode → decode → encode byte-identity.
 */
class CodecRoundTripTest {

    private static Dictionary dict() {
        return Dictionary.load(
                CodecRoundTripTest.class.getResourceAsStream("/fields.yaml"),
                CodecRoundTripTest.class.getResourceAsStream("/messages.yaml"));
    }

    @Test
    void roundTripAllMessageTypesWithEveryField() {
        Dictionary dict = dict();

        for (Dictionary.MessageDef def : dict.allMessages()) {
            Message msg = new Message(def.type()).compId("TEST").sequenceNumber(7);

            Map<String, Object> expected = new LinkedHashMap<>();
            for (Dictionary.FieldEntry entry : def.fields()) {
                Object v = sampleValue(dict.field(entry.field()));
                msg.put(entry.field(), v);
                expected.put(entry.field(), v);
            }

            byte[] frame = MessageCodec.encode(dict, msg);
            Message decoded = MessageCodec.decode(dict, frame);

            assertEquals(def.type(), decoded.messageType());
            assertEquals("TEST", decoded.compId());
            assertEquals(7L, decoded.sequenceNumber());

            for (Map.Entry<String, Object> e : expected.entrySet()) {
                Object got = decoded.get(e.getKey());
                assertEquals(e.getValue(), got, def.name() + "." + e.getKey());
            }

            // re-encode the decoded message must be byte-identical
            byte[] frame2 = MessageCodec.encode(dict, decoded);
            assertArrayEquals(frame, frame2, def.name() + " re-encode mismatch");
        }
    }

    @Test
    void decimalBoundaries() {
        Dictionary dict = dict();
        long[] values = {0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 1250000000L};
        for (long v : values) {
            Message m = new Message(11).compId("T").put("price", v);
            byte[] frame = MessageCodec.encode(dict, m);
            assertEquals(v, MessageCodec.decode(dict, frame).get("price"));
        }
    }

    @Test
    void emptyAlphaVarRoundTrip() {
        Dictionary dict = dict();
        // Logout.logoutText is alpha-var; empty string → length 1 (null only)
        Message m = new Message(6).compId("T").put("logoutText", "");
        byte[] frame = MessageCodec.encode(dict, m);
        assertEquals("", MessageCodec.decode(dict, frame).get("logoutText"));
    }

    @Test
    void emptyAlphaFixedRoundTrip() {
        Dictionary dict = dict();
        // LookupResponse.reason is alpha-var, but exercise a fixed field with empty value
        Message m = new Message(11).compId("T").put("securityExchange", "");
        byte[] frame = MessageCodec.encode(dict, m);
        assertEquals("", MessageCodec.decode(dict, frame).get("securityExchange"));
    }

    @Test
    void absentFieldsDecodeToNull() {
        Dictionary dict = dict();
        // Heartbeat with no body fields → decode yields no fields
        Message m = new Message(0).compId("T");
        byte[] frame = MessageCodec.encode(dict, m);
        Message decoded = MessageCodec.decode(dict, frame);
        assertNull(decoded.get("referenceTestRequestId"));
    }

    private static Object sampleValue(Dictionary.FieldDef fd) {
        return switch (fd.type()) {
            case UINT8 -> 200L;
            case INT8 -> -5L;
            case UINT16 -> 60000L;
            case INT16 -> -30000L;
            case UINT32 -> 4000000000L;
            case INT32 -> -2000000000L;
            case UINT64, INT64 -> Long.MAX_VALUE;
            case DECIMAL -> -123456789L;
            case BYTE -> "F";
            case ALPHA_FIXED -> "TEST";
            case ALPHA_VAR -> "hello";
            case BITMAP_FIXED -> new byte[32];
            case BITMAP_VAR -> new byte[2];
        };
    }
}
