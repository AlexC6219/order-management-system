package com.hase.oms.codec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageCodecTest {

    private static Dictionary dict() {
        return Dictionary.load(
                MessageCodecTest.class.getResourceAsStream("/fields.yaml"),
                MessageCodecTest.class.getResourceAsStream("/messages.yaml"));
    }

    @Test
    void heartbeatGoldenVector() {
        Dictionary dict = dict();
        Message hb = new Message(0).compId("TEST");

        byte[] frame = MessageCodec.encode(dict, hb);

        // total = header(54) + body(0) + trailer(4) = 58
        assertEquals(58, frame.length);

        byte[] expected = new byte[58];
        expected[0] = 0x02;                       // STX
        expected[1] = 0x3A; expected[2] = 0x00;   // Length = 58 (LE)
        expected[3] = 0x00;                       // MessageType = 0
        expected[4] = 0x01;                       // SeqNum = 1 (LE)
        expected[5] = 0x00; expected[6] = 0x00; expected[7] = 0x00;
        expected[8] = 0x00;                       // PossDup
        expected[9] = 0x00;                       // PossResend
        expected[10] = 'T'; expected[11] = 'E'; expected[12] = 'S'; expected[13] = 'T';
        // CompId null-padded to 12 (bytes 10..21), presence map 32 zero bytes (22..53)
        // checksum occupies 54..57

        assertArrayEquals(java.util.Arrays.copyOf(expected, 54),
                java.util.Arrays.copyOf(frame, 54));

        int crc = Crc32c.compute(frame, 0, 54);
        assertEquals(crc, readTrailingU32(frame, 54));
    }

    @Test
    void heartbeatPresenceMapMsbFirst() {
        Dictionary dict = dict();
        // referenceTestRequestId is bit 0 -> most-significant bit of byte 0
        Message hb = new Message(0).compId("TEST").put("referenceTestRequestId", 1234L);
        byte[] frame = MessageCodec.encode(dict, hb);

        // presence map starts at offset 22 (after 1+2+1+4+1+1+12)
        assertEquals(0x80, frame[22] & 0xFF);
        // then referenceTestRequestId as UInt16 LE = 1234 = 0x04D2 -> D2 04
        assertEquals(0xD2, frame[54] & 0xFF);
        assertEquals(0x04, frame[55] & 0xFF);
    }

    @Test
    void newOrderPresenceMapAndRoundTrip() {
        Dictionary dict = dict();
        Message order = new Message(11).compId("TEST")
                .put("clientOrderId", "1000")
                .put("submittingBrokerId", "4")
                .put("securityId", "00005")
                .put("securityIdSource", 8L)
                .put("securityExchange", "XHKG")
                .put("transactionTime", "20260101-09:30:00.000000")
                .put("side", 1L)
                .put("orderType", 2L)
                .put("price", 1250000000L)          // 12.50
                .put("orderQuantity", 50000000000L) // 500
                .put("disclosureInstructions", 1L);

        byte[] frame = MessageCodec.encode(dict, order);

        // presence map (offset 22): bits 0-4 set, 5 clear, 6,7 set = 0xFB
        assertEquals(0xFB, frame[22] & 0xFF);
        // bits 8,9,10 set = 0xE0
        assertEquals(0xE0, frame[23] & 0xFF);
        // bit 18 set (byte 2, MSB-first -> 0x20)
        assertEquals(0x20, frame[24] & 0xFF);

        Message decoded = MessageCodec.decode(dict, frame);
        assertEquals(11, decoded.messageType());
        assertEquals("1000", decoded.get("clientOrderId"));
        assertEquals("4", decoded.get("submittingBrokerId"));
        assertEquals("00005", decoded.get("securityId"));
        assertEquals(8L, decoded.get("securityIdSource"));
        assertEquals("XHKG", decoded.get("securityExchange"));
        assertEquals(1L, decoded.get("side"));
        assertEquals(2L, decoded.get("orderType"));
        assertEquals(1250000000L, decoded.get("price"));
        assertEquals(50000000000L, decoded.get("orderQuantity"));
        assertEquals(1L, decoded.get("disclosureInstructions"));
    }

    @Test
    void executionReportRoundTrip() {
        Dictionary dict = dict();
        Message er = new Message(10).compId("TEST")
                .put("clientOrderId", "1000")
                .put("submittingBrokerId", "4")
                .put("securityId", "00005")
                .put("securityIdSource", 8L)
                .put("securityExchange", "XHKG")
                .put("transactionTime", "20260101-09:30:00.000000")
                .put("side", 1L)
                .put("orderId", "2000")
                .put("executionId", "Exe1000")
                .put("orderStatus", 0L)
                .put("execType", "0")
                .put("cumulativeQuantity", 0L)
                .put("leavesQuantity", 50000000000L);

        byte[] frame = MessageCodec.encode(dict, er);
        Message decoded = MessageCodec.decode(dict, frame);

        assertEquals("1000", decoded.get("clientOrderId"));
        assertEquals("2000", decoded.get("orderId"));
        assertEquals("Exe1000", decoded.get("executionId"));
        assertEquals(0L, decoded.get("orderStatus"));
        assertEquals("0", decoded.get("execType")); // byte type decoded as single-char string
        assertEquals(50000000000L, decoded.get("leavesQuantity"));
    }

    @Test
    void rejectOnBadChecksum() {
        Dictionary dict = dict();
        Message hb = new Message(0).compId("TEST");
        byte[] frame = MessageCodec.encode(dict, hb);
        frame[58 - 1] ^= 0xFF; // corrupt last checksum byte

        assertThrows(IllegalArgumentException.class, () -> MessageCodec.decode(dict, frame));
    }

    @Test
    void rejectOnBadStx() {
        Dictionary dict = dict();
        Message hb = new Message(0).compId("TEST");
        byte[] frame = MessageCodec.encode(dict, hb);
        frame[0] = 0x03;

        assertThrows(IllegalArgumentException.class, () -> MessageCodec.decode(dict, frame));
    }

    @Test
    void roundTripAllAdminMessages() {
        Dictionary dict = dict();
        // exercise encode->decode->encode identity across several admin message types
        Message[] msgs = {
                new Message(1).compId("TEST").put("testRequestId", 42L),
                new Message(2).compId("TEST").put("startSequence", 7L).put("endSequence", 0L),
                new Message(3).compId("TEST").put("messageRejectCode", 5L).put("referenceSequenceNumber", 3L),
                new Message(4).compId("TEST").put("gapFill", "Y").put("newSequenceNumber", 100L),
                new Message(5).compId("TEST").put("nextExpectedMessageSequence", 1L).put("sessionStatus", 0L),
                new Message(6).compId("TEST").put("logoutText", "bye"),
                new Message(7).compId("TEST").put("typeOfService", 1L).put("protocolType", 1L),
                new Message(8).compId("TEST").put("status", 0L).put("primaryIp", "127.0.0.1").put("primaryPort", 9000L),
        };

        for (Message m : msgs) {
            byte[] frame = MessageCodec.encode(dict, m);
            Message decoded = MessageCodec.decode(dict, frame);
            byte[] frame2 = MessageCodec.encode(dict, decoded);
            assertArrayEquals(frame, frame2, "round-trip failed for type " + m.messageType());
            assertTrue(m.fields().entrySet().stream()
                    .allMatch(e -> e.getValue().equals(decoded.get(e.getKey()))));
        }
    }

    private static int readTrailingU32(byte[] data, int offset) {
        return (data[offset] & 0xFF)
                | ((data[offset + 1] & 0xFF) << 8)
                | ((data[offset + 2] & 0xFF) << 16)
                | ((data[offset + 3] & 0xFF) << 24);
    }
}
