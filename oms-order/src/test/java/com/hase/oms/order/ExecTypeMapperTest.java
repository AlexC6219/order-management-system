package com.hase.oms.order;

import com.hase.oms.codec.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExecTypeMapperTest {

    private static Message er(String execType) {
        return new Message(10).put("execType", execType).put("executionId", "E1").put("orderId", "O1");
    }

    @Test
    void mapsEveryExecType() {
        assertInstanceOf(OrderEvent.NewAck.class, ExecTypeMapper.toEvent(er("0")));
        assertInstanceOf(OrderEvent.Reject.class, ExecTypeMapper.toEvent(er("8")));
        assertInstanceOf(OrderEvent.Expire.class, ExecTypeMapper.toEvent(er("C")));
        assertInstanceOf(OrderEvent.Fill.class, ExecTypeMapper.toEvent(er("F")));
        assertInstanceOf(OrderEvent.Cancel.class, ExecTypeMapper.toEvent(er("4")));
        assertInstanceOf(OrderEvent.Amend.class, ExecTypeMapper.toEvent(er("5")));
        assertInstanceOf(OrderEvent.TradeCancel.class, ExecTypeMapper.toEvent(er("H")));
        assertInstanceOf(OrderEvent.CancelReject.class, ExecTypeMapper.toEvent(er("X")));
        assertInstanceOf(OrderEvent.AmendReject.class, ExecTypeMapper.toEvent(er("Y")));
    }

    @Test
    void fillCarriesQuantities() {
        Message er = er("F")
                .put("cumulativeQuantity", 400_00000000L)
                .put("leavesQuantity", 600_00000000L)
                .put("executionQuantity", 400_00000000L)
                .put("executionPrice", 78_50000000L);
        OrderEvent.Fill fill = (OrderEvent.Fill) ExecTypeMapper.toEvent(er);
        assertEquals(400_00000000L, fill.cumulativeQuantity());
        assertEquals(600_00000000L, fill.leavesQuantity());
    }

    @Test
    void unknownExecTypeRejected() {
        assertThrows(IllegalArgumentException.class, () -> ExecTypeMapper.toEvent(er("Z")));
    }
}
