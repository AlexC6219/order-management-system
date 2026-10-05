package com.hase.oms.order;

/**
 * Events applied to an {@link Order} by the {@link OrderStateMachine}.
 *
 * <p>These are internal; the OCG-C {@code ExecutionReport} is translated into
 * one of these by {@link ExecTypeMapper}.
 */
public sealed interface OrderEvent {

    /** Venue accepted the new order (Exec Type {@code '0'}). */
    record NewAck(String exchangeOrderId) implements OrderEvent {}

    /** A trade (full or partial) occurred (Exec Type {@code 'F'}). */
    record Fill(String executionId, long cumulativeQuantity, long leavesQuantity,
                long lastQuantity, long lastPrice) implements OrderEvent {}

    /** Order cancelled, possibly unsolicited (Exec Type {@code '4'}). */
    record Cancel(String executionId, Long restatementReason) implements OrderEvent {}

    /** Amend accepted (Exec Type {@code '5'}). */
    record Amend(String executionId, String exchangeOrderId) implements OrderEvent {}

    /** Order rejected (Exec Type {@code '8'}). */
    record Reject(long rejectCode, String reason) implements OrderEvent {}

    /** Order expired (Exec Type {@code 'C'}). */
    record Expire(String executionId) implements OrderEvent {}

    /** A previously reported trade was busted (Exec Type {@code 'H'}). */
    record TradeCancel(String executionId, long cumulativeQuantity, long leavesQuantity)
            implements OrderEvent {}

    /** Cancel was refused (Exec Type {@code 'X'}). */
    record CancelReject(String executionId, long rejectCode) implements OrderEvent {}

    /** Amend was refused (Exec Type {@code 'Y'}). */
    record AmendReject(String executionId, long rejectCode) implements OrderEvent {}
}
