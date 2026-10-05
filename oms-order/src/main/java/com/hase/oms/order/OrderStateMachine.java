package com.hase.oms.order;

/**
 * Applies {@link OrderEvent}s to an {@link Order} and enforces lifecycle rules
 * (DESIGN.md §8.1).
 *
 * <p>Invalid or stale events are ignored (return {@code false}) rather than
 * throwing, so a replayed/superseded Execution Report cannot corrupt state.
 * Trade Cancel on a {@code FILLED} order is valid (bust); on any other terminal
 * state it is ignored.
 */
public final class OrderStateMachine {

    /** Marks an order as awaiting an amend reply. */
    public boolean requestAmend(Order order) {
        if (order.state().isTerminal() || order.state().isPending()) {
            return false;
        }
        order.state(OrderState.PENDING_REPLACE);
        return true;
    }

    /** Marks an order as awaiting a cancel reply. */
    public boolean requestCancel(Order order) {
        if (order.state().isTerminal() || order.state().isPending()) {
            return false;
        }
        order.state(OrderState.PENDING_CANCEL);
        return true;
    }

    public boolean apply(Order order, OrderEvent event) {
        OrderState state = order.state();

        if (state.isTerminal()) {
            // Only a trade bust may revive a filled order.
            if (event instanceof OrderEvent.TradeCancel tc && state == OrderState.FILLED) {
                order.applyTradeCancel(tc.cumulativeQuantity(), tc.leavesQuantity(), tc.executionId());
                order.state(order.liveState());
                return true;
            }
            return false;
        }

        return switch (event) {
            case OrderEvent.NewAck ack -> onNewAck(order, ack);
            case OrderEvent.Fill fill -> onFill(order, fill);
            case OrderEvent.Cancel cancel -> onCancel(order, cancel);
            case OrderEvent.Amend amend -> onAmend(order, amend);
            case OrderEvent.Reject reject -> onReject(order, reject);
            case OrderEvent.Expire expire -> onExpire(order, expire);
            case OrderEvent.TradeCancel tc -> onTradeCancel(order, tc);
            case OrderEvent.CancelReject cr -> onCancelReject(order, cr);
            case OrderEvent.AmendReject ar -> onAmendReject(order, ar);
        };
    }

    private boolean onNewAck(Order order, OrderEvent.NewAck ack) {
        if (order.state() != OrderState.PENDING_NEW) {
            return false;
        }
        order.exchangeOrderId(ack.exchangeOrderId());
        order.state(OrderState.NEW);
        return true;
    }

    private boolean onFill(Order order, OrderEvent.Fill fill) {
        switch (order.state()) {
            case NEW, PARTIALLY_FILLED, PENDING_CANCEL, PENDING_REPLACE -> {
                order.applyFill(fill.cumulativeQuantity(), fill.leavesQuantity(), fill.executionId());
                order.state(order.liveState());
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean onCancel(Order order, OrderEvent.Cancel cancel) {
        switch (order.state()) {
            case PENDING_NEW, NEW, PARTIALLY_FILLED, PENDING_CANCEL, PENDING_REPLACE -> {
                order.lastExecutionId(cancel.executionId());
                order.state(OrderState.CANCELLED);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean onAmend(Order order, OrderEvent.Amend amend) {
        if (order.state() != OrderState.PENDING_REPLACE) {
            return false;
        }
        if (amend.exchangeOrderId() != null) {
            order.exchangeOrderId(amend.exchangeOrderId());
        }
        order.lastExecutionId(amend.executionId());
        order.state(order.liveState());
        return true;
    }

    private boolean onReject(Order order, OrderEvent.Reject reject) {
        if (order.state() != OrderState.PENDING_NEW) {
            return false;
        }
        order.state(OrderState.REJECTED);
        return true;
    }

    private boolean onExpire(Order order, OrderEvent.Expire expire) {
        switch (order.state()) {
            case PENDING_NEW, NEW, PARTIALLY_FILLED, PENDING_CANCEL, PENDING_REPLACE -> {
                order.lastExecutionId(expire.executionId());
                order.state(OrderState.EXPIRED);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean onTradeCancel(Order order, OrderEvent.TradeCancel tc) {
        switch (order.state()) {
            case NEW, PARTIALLY_FILLED, FILLED, PENDING_CANCEL, PENDING_REPLACE -> {
                // Apply the venue's quantities directly; never recompute leaves
                // as quantity - cum (a bust must not reinstate leaves).
                order.applyTradeCancel(tc.cumulativeQuantity(), tc.leavesQuantity(), tc.executionId());
                order.state(order.liveState());
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean onCancelReject(Order order, OrderEvent.CancelReject cr) {
        if (order.state() != OrderState.PENDING_CANCEL) {
            return false;
        }
        order.lastExecutionId(cr.executionId());
        order.state(order.liveState());
        return true;
    }

    private boolean onAmendReject(Order order, OrderEvent.AmendReject ar) {
        if (order.state() != OrderState.PENDING_REPLACE) {
            return false;
        }
        order.lastExecutionId(ar.executionId());
        order.state(order.liveState());
        return true;
    }
}
