package com.hase.oms.order;

import com.hase.oms.codec.Dictionary;
import com.hase.oms.codec.Message;
import com.hase.oms.codec.MessageCodec;
import com.hase.oms.codec.enums.OrderType;
import com.hase.oms.codec.enums.Side;
import com.hase.oms.codec.enums.Tif;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTranslatorTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-03-04T05:06:07.123456Z"), ZoneOffset.UTC);

    private static Dictionary dict() {
        return Dictionary.load(
                OrderTranslatorTest.class.getResourceAsStream("/fields.yaml"),
                OrderTranslatorTest.class.getResourceAsStream("/messages.yaml"));
    }

    private static OrderTranslator translator() {
        return new OrderTranslator(dict(), "TEST", FIXED);
    }

    private static Order order() {
        return new Order(42L, "BROKER1", "5", Side.Buy, OrderType.Limit, Tif.Day,
                78_50000000L, 1000_00000000L);
    }

    @Test
    void newOrderCarriesRequiredFields() {
        Message m = translator().newOrder(order(), 7);
        assertEquals(11, m.messageType());
        assertEquals(7L, m.sequenceNumber());
        assertEquals("42", m.get("clientOrderId"));
        assertEquals("BROKER1", m.get("submittingBrokerId"));
        assertEquals("5", m.get("securityId"));
        assertEquals(8L, m.get("securityIdSource"));
        assertEquals("XHKG", m.get("securityExchange"));
        assertEquals(1L, m.get("side"));
        assertEquals(2L, m.get("orderType"));
        assertEquals(78_50000000L, m.get("price"));
        assertEquals(1000_00000000L, m.get("orderQuantity"));
        assertEquals("20260304-05:06:07.123456", m.get("transactionTime"));
    }

    @Test
    void newOrderRoundTripsThroughCodec() {
        Dictionary dict = dict();
        Message m = translator().newOrder(order(), 7);
        byte[] frame = MessageCodec.encode(dict, m);
        Message decoded = MessageCodec.decode(dict, frame);
        assertEquals(11, decoded.messageType());
        assertEquals("42", decoded.get("clientOrderId"));
        assertEquals(78_50000000L, decoded.get("price"));
        assertEquals("TEST", decoded.compId());
    }

    @Test
    void amendLinksToOriginal() {
        Message m = translator().amend(order(), 43L, 42L, 8);
        assertEquals(12, m.messageType());
        assertEquals("43", m.get("clientOrderId"));
        assertEquals("42", m.get("originalClientOrderId"));
    }

    @Test
    void cancelLinksToOriginal() {
        Message m = translator().cancel(order(), 43L, 42L, 9);
        assertEquals(13, m.messageType());
        assertEquals("43", m.get("clientOrderId"));
        assertEquals("42", m.get("originalClientOrderId"));
    }

    @Test
    void massCancelAllOrders() {
        Message m = translator().massCancel(99L, "BROKER1", 10);
        assertEquals(14, m.messageType());
        assertEquals(7L, m.get("massCancelRequestType"));
    }

    @Test
    void decodeExecutionReport() {
        Message er = new Message(10).put("execType", "F").put("executionId", "E1")
                .put("cumulativeQuantity", 400_00000000L).put("leavesQuantity", 600_00000000L);
        OrderEvent event = translator().decodeExecutionReport(er);
        assertTrue(event instanceof OrderEvent.Fill);
    }
}
