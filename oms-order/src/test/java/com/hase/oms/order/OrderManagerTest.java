package com.hase.oms.order;

import com.hase.oms.codec.Message;
import com.hase.oms.codec.enums.OrderType;
import com.hase.oms.codec.enums.Side;
import com.hase.oms.codec.enums.Tif;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderManagerTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-03-04T05:06:07Z"), ZoneOffset.UTC);

    private final List<Message> outbound = new ArrayList<>();
    private final OrderManager manager;

    OrderManagerTest() {
        OrderTranslator translator = new OrderTranslator("TEST", FIXED);
        manager = new OrderManager(new ClientOrderIdAllocator(), translator, outbound::add);
    }

    private static OrderRequest request() {
        return new OrderRequest("UP-1", "BROKER1", "5", Side.Buy, OrderType.Limit, Tif.Day,
                78_50000000L, 1000_00000000L, null);
    }

    private static Message er(long clientOrderId, String execType, String executionId) {
        return new Message(10)
                .put("clientOrderId", Long.toString(clientOrderId))
                .put("execType", execType)
                .put("executionId", executionId);
    }

    @Test
    void submitAllocatesIdAndEmitsNewOrder() {
        Order order = manager.submit(request());
        assertEquals(1L, order.clientOrderId());
        assertEquals(OrderState.PENDING_NEW, order.state());
        assertEquals(1, outbound.size());
        assertEquals(11, outbound.get(0).messageType());
        assertEquals("1", outbound.get(0).get("clientOrderId"));
    }

    @Test
    void duplicateUpstreamClOrdIdRejected() {
        manager.submit(request());
        assertThrows(IllegalArgumentException.class, () -> manager.submit(request()));
    }

    @Test
    void newAckThenFillUpdatesState() {
        Order order = manager.submit(request());
        manager.onExecutionReport(er(1, "0", "E0").put("orderId", "O1"));
        assertEquals(OrderState.NEW, order.state());

        manager.onExecutionReport(er(1, "F", "E1")
                .put("cumulativeQuantity", 1000_00000000L).put("leavesQuantity", 0L));
        assertEquals(OrderState.FILLED, order.state());
    }

    @Test
    void duplicateExecutionIdIgnored() {
        Order order = manager.submit(request());
        manager.onExecutionReport(er(1, "0", "E0").put("orderId", "O1"));
        assertTrue(manager.onExecutionReport(er(1, "F", "E1")
                .put("cumulativeQuantity", 400_00000000L).put("leavesQuantity", 600_00000000L)));
        assertFalse(manager.onExecutionReport(er(1, "F", "E1")
                .put("cumulativeQuantity", 400_00000000L).put("leavesQuantity", 600_00000000L)));
    }

    @Test
    void inconsistentFillDetected() {
        manager.submit(request());
        manager.onExecutionReport(er(1, "0", "E0").put("orderId", "O1"));
        assertThrows(InconsistentExecutionException.class, () ->
                manager.onExecutionReport(er(1, "F", "E1")
                        .put("cumulativeQuantity", 400_00000000L).put("leavesQuantity", 500_00000000L)));
    }

    @Test
    void cancelConsumesFreshIdAndLinksChain() {
        Order order = manager.submit(request());
        manager.onExecutionReport(er(1, "0", "E0").put("orderId", "O1"));
        long newId = manager.cancel(order);
        assertEquals(2L, newId);
        assertEquals(OrderState.PENDING_CANCEL, order.state());
        Message cancel = outbound.get(outbound.size() - 1);
        assertEquals(13, cancel.messageType());
        assertEquals("2", cancel.get("clientOrderId"));
        assertEquals("1", cancel.get("originalClientOrderId"));
    }

    @Test
    void amendConsumesFreshIdAndUpdatesOrder() {
        Order order = manager.submit(request());
        manager.onExecutionReport(er(1, "0", "E0").put("orderId", "O1"));
        long newId = manager.amend(order, 80_00000000L, 500_00000000L);
        assertEquals(2L, newId);
        assertEquals(OrderState.PENDING_REPLACE, order.state());
        assertEquals(80_00000000L, order.price());
        assertEquals(500_00000000L, order.quantity());
    }

    @Test
    void massCancelEmitsRequest() {
        long id = manager.massCancel("BROKER1");
        assertEquals(1L, id);
        assertEquals(14, outbound.get(0).messageType());
    }
}
