package com.hase.oms.order;

import com.hase.oms.codec.Dictionary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStateMachineTest {

    private final OrderStateMachine sm = new OrderStateMachine();

    private static Order order() {
        return new Order(1L, "BROKER1", "5", com.hase.oms.codec.enums.Side.Buy,
                com.hase.oms.codec.enums.OrderType.Limit, com.hase.oms.codec.enums.Tif.Day,
                78_50000000L, 1000_00000000L);
    }

    @Test
    void newAckMovesPendingNewToNew() {
        Order order = order();
        assertTrue(sm.apply(order, new OrderEvent.NewAck("O1")));
        assertEquals(OrderState.NEW, order.state());
        assertEquals("O1", order.exchangeOrderId());
    }

    @Test
    void rejectOnlyFromPendingNew() {
        Order order = order();
        assertTrue(sm.apply(order, new OrderEvent.Reject(13, "qty")));
        assertEquals(OrderState.REJECTED, order.state());
        assertFalse(sm.apply(order, new OrderEvent.NewAck("O1")));
    }

    @Test
    void partialThenFullFill() {
        Order order = order();
        sm.apply(order, new OrderEvent.NewAck("O1"));

        sm.apply(order, new OrderEvent.Fill("E1", 400_00000000L, 600_00000000L, 400_00000000L, 78_50000000L));
        assertEquals(OrderState.PARTIALLY_FILLED, order.state());

        sm.apply(order, new OrderEvent.Fill("E2", 1000_00000000L, 0L, 600_00000000L, 78_50000000L));
        assertEquals(OrderState.FILLED, order.state());
        assertTrue(order.state().isTerminal());
    }

    @Test
    void cancelFromLive() {
        Order order = order();
        sm.apply(order, new OrderEvent.NewAck("O1"));
        assertTrue(sm.apply(order, new OrderEvent.Cancel("E1", null)));
        assertEquals(OrderState.CANCELLED, order.state());
    }

    @Test
    void requestCancelThenCancelRejectRevertsToLiveState() {
        Order order = order();
        sm.apply(order, new OrderEvent.NewAck("O1"));
        assertTrue(sm.requestCancel(order));
        assertEquals(OrderState.PENDING_CANCEL, order.state());

        assertTrue(sm.apply(order, new OrderEvent.CancelReject("E1", 0)));
        assertEquals(OrderState.NEW, order.state());
    }

    @Test
    void amendRoundTrip() {
        Order order = order();
        sm.apply(order, new OrderEvent.NewAck("O1"));
        assertTrue(sm.requestAmend(order));
        assertEquals(OrderState.PENDING_REPLACE, order.state());
        assertTrue(sm.apply(order, new OrderEvent.Amend("E1", "O2")));
        assertEquals(OrderState.NEW, order.state());
        assertEquals("O2", order.exchangeOrderId());
    }

    @Test
    void expireFromLive() {
        Order order = order();
        sm.apply(order, new OrderEvent.NewAck("O1"));
        assertTrue(sm.apply(order, new OrderEvent.Expire("E1")));
        assertEquals(OrderState.EXPIRED, order.state());
    }

    @Test
    void tradeCancelOnFilledDoesNotReinstateLeaves() {
        Order order = order();
        sm.apply(order, new OrderEvent.NewAck("O1"));
        sm.apply(order, new OrderEvent.Fill("E1", 1000_00000000L, 0L, 1000_00000000L, 78_50000000L));
        assertEquals(OrderState.FILLED, order.state());

        // Bust 400 of the filled quantity; venue reports cum 600 and leaves 400.
        assertTrue(sm.apply(order, new OrderEvent.TradeCancel("E2", 600_00000000L, 400_00000000L)));
        assertEquals(OrderState.PARTIALLY_FILLED, order.state());
        assertEquals(600_00000000L, order.cumulativeQuantity());
        assertEquals(400_00000000L, order.leavesQuantity());
        assertFalse(order.leavesQuantity() > order.quantity(), "leaves must not exceed quantity");
    }

    @Test
    void staleEventOnTerminalIgnored() {
        Order order = order();
        sm.apply(order, new OrderEvent.NewAck("O1"));
        sm.apply(order, new OrderEvent.Cancel("E1", null));
        assertFalse(sm.apply(order, new OrderEvent.Fill("E2", 100_00000000L, 0L, 100_00000000L, 1L)));
    }

    @Test
    void duplicateNewAckIgnored() {
        Order order = order();
        assertTrue(sm.apply(order, new OrderEvent.NewAck("O1")));
        assertFalse(sm.apply(order, new OrderEvent.NewAck("O1")));
    }
}
