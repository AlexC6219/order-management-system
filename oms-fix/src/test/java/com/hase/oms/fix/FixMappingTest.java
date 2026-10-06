package com.hase.oms.fix;

import com.hase.oms.codec.enums.OrderType;
import com.hase.oms.codec.enums.Side;
import com.hase.oms.codec.enums.Tif;
import com.hase.oms.order.Order;
import com.hase.oms.order.OrderEvent;
import com.hase.oms.order.OrderRequest;
import com.hase.oms.order.OrderStateMachine;
import org.junit.jupiter.api.Test;
import quickfix.FieldNotFound;
import quickfix.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixMappingTest {

    private final OrderStateMachine sm = new OrderStateMachine();

    private Order order() {
        Order order = new Order(42L, "BROKER1", "5", Side.Buy, OrderType.Limit, Tif.Day,
                78_50000000L, 1000_00000000L);
        return order;
    }

    private Order liveOrder() {
        Order order = order();
        sm.apply(order, new OrderEvent.NewAck("O1"));
        return order;
    }

    @Test
    void parsesNewOrderSingle() throws FieldNotFound {
        Message nos = new Message();
        nos.getHeader().setString(FixTags.MSG_TYPE, FixTags.NEW_ORDER_SINGLE);
        nos.setString(FixTags.CL_ORD_ID, "UP-1");
        nos.setString(FixTags.SYMBOL, "5");
        nos.setChar(FixTags.SIDE, '1');
        nos.setChar(FixTags.ORD_TYPE, '2');
        nos.setDouble(FixTags.ORDER_QTY, 1000.0, 8);
        nos.setDouble(FixTags.PRICE, 78.5, 8);
        nos.setChar(FixTags.TIME_IN_FORCE, '0');
        nos.setString(FixTags.ACCOUNT, "BROKER1");

        OrderRequest request = FixMapping.toOrderRequest(nos);
        assertEquals("UP-1", request.upstreamClOrdId());
        assertEquals("BROKER1", request.submittingBrokerId());
        assertEquals("5", request.securityId());
        assertEquals(Side.Buy, request.side());
        assertEquals(OrderType.Limit, request.orderType());
        assertEquals(Tif.Day, request.tif());
        assertEquals(78_50000000L, request.price());
        assertEquals(1000_00000000L, request.quantity());
    }

    @Test
    void fillMapsToExecutionReport() throws Exception {
        Order order = liveOrder();
        sm.apply(order, new OrderEvent.Fill("E1", 1000_00000000L, 0L, 1000_00000000L, 78_50000000L));

        Message er = FixMapping.executionReport(order, new OrderEvent.Fill("E1",
                1000_00000000L, 0L, 1000_00000000L, 78_50000000L));

        assertEquals(FixTags.EXECUTION_REPORT, er.getHeader().getString(FixTags.MSG_TYPE));
        assertEquals('F', er.getChar(FixTags.EXEC_TYPE));
        assertEquals('2', er.getChar(FixTags.ORD_STATUS));
        assertEquals("42", er.getString(FixTags.CL_ORD_ID));
        assertEquals("O1", er.getString(FixTags.ORDER_ID));
        assertEquals("E1", er.getString(FixTags.EXEC_ID));
    }

    @Test
    void partialFillOrdStatus() throws Exception {
        Order order = liveOrder();
        sm.apply(order, new OrderEvent.Fill("E1", 400_00000000L, 600_00000000L, 400_00000000L, 78_50000000L));
        Message er = FixMapping.executionReport(order, new OrderEvent.Fill("E1",
                400_00000000L, 600_00000000L, 400_00000000L, 78_50000000L));
        assertEquals('F', er.getChar(FixTags.EXEC_TYPE));
        assertEquals('1', er.getChar(FixTags.ORD_STATUS));
    }

    @Test
    void cancelRejectSynthesizesOrderCancelReject() throws Exception {
        Order order = liveOrder();
        Message reject = FixMapping.orderCancelReject(order, new OrderEvent.CancelReject("E1", 0));
        assertEquals(FixTags.ORDER_CANCEL_REJECT, reject.getHeader().getString(FixTags.MSG_TYPE));
        assertEquals('1', reject.getChar(FixTags.CXL_REJ_RESPONSE_TO));
        assertEquals("42", reject.getString(FixTags.CL_ORD_ID));
    }

    @Test
    void amendRejectSynthesizesOrderCancelReject() throws Exception {
        Order order = liveOrder();
        Message reject = FixMapping.orderCancelReject(order, new OrderEvent.AmendReject("E1", 8));
        assertEquals('2', reject.getChar(FixTags.CXL_REJ_RESPONSE_TO));
        assertEquals(8, reject.getInt(FixTags.CXL_REJ_REASON));
    }

    @Test
    void newAckMapsExecTypeZero() throws Exception {
        Order order = liveOrder();
        Message er = FixMapping.executionReport(order, new OrderEvent.NewAck("O1"));
        assertEquals('0', er.getChar(FixTags.EXEC_TYPE));
        assertEquals('0', er.getChar(FixTags.ORD_STATUS));
    }
}
