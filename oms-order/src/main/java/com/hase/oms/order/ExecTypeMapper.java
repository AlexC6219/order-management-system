package com.hase.oms.order;

import com.hase.oms.codec.Message;

/**
 * Translates an OCG-C {@code ExecutionReport (10)} into an {@link OrderEvent}
 * (DESIGN.md §8.2). Unknown Exec Types are rejected.
 */
public final class ExecTypeMapper {

    private ExecTypeMapper() {}

    public static OrderEvent toEvent(Message er) {
        String execType = String.valueOf(er.get("execType"));
        if (execType == null || execType.isEmpty()) {
            throw new IllegalArgumentException("ExecutionReport missing execType");
        }
        return switch (execType.charAt(0)) {
            case '0' -> new OrderEvent.NewAck(string(er.get("orderId")));
            case '8' -> new OrderEvent.Reject(number(er.get("orderRejectCode")), string(er.get("reason")));
            case 'C' -> new OrderEvent.Expire(string(er.get("executionId")));
            case 'F' -> new OrderEvent.Fill(
                    string(er.get("executionId")),
                    number(er.get("cumulativeQuantity")),
                    number(er.get("leavesQuantity")),
                    number(er.get("executionQuantity")),
                    number(er.get("executionPrice")));
            case '4' -> new OrderEvent.Cancel(
                    string(er.get("executionId")),
                    er.has("execRestatementReason") ? number(er.get("execRestatementReason")) : null);
            case '5' -> new OrderEvent.Amend(string(er.get("executionId")), string(er.get("orderId")));
            case 'H' -> new OrderEvent.TradeCancel(
                    string(er.get("executionId")),
                    number(er.get("cumulativeQuantity")),
                    number(er.get("leavesQuantity")));
            case 'X' -> new OrderEvent.CancelReject(
                    string(er.get("executionId")), number(er.get("cancelRejectCode")));
            case 'Y' -> new OrderEvent.AmendReject(
                    string(er.get("executionId")), number(er.get("amendRejectCode")));
            default -> throw new IllegalArgumentException("Unknown Exec Type: " + execType);
        };
    }

    private static long number(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static String string(Object value) {
        return value == null ? null : (String) value;
    }
}
