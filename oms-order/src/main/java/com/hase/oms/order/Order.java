package com.hase.oms.order;

import com.hase.oms.codec.enums.OrderType;
import com.hase.oms.codec.enums.Side;
import com.hase.oms.codec.enums.Tif;

/**
 * Mutable order aggregate owned by the {@link OrderStateMachine}.
 *
 * <p>Prices and quantities are OCG-C decimals: signed 64-bit integers scaled by
 * 1e8. {@code originalClientOrderId} links an amend/cancel to the prior order in
 * the chain (DESIGN.md §9.2).
 */
public final class Order {

    private final long clientOrderId;
    private final String submittingBrokerId;
    private final String securityId;
    private final Side side;
    private final OrderType orderType;
    private final Tif tif;

    private Long originalClientOrderId;
    private String exchangeOrderId;
    private long price;
    private long quantity;
    private long cumulativeQuantity;
    private long leavesQuantity;
    private OrderState state;
    private String lastExecutionId;

    public Order(long clientOrderId, String submittingBrokerId, String securityId,
                 Side side, OrderType orderType, Tif tif, long price, long quantity) {
        this.clientOrderId = clientOrderId;
        this.submittingBrokerId = submittingBrokerId;
        this.securityId = securityId;
        this.side = side;
        this.orderType = orderType;
        this.tif = tif;
        this.price = price;
        this.quantity = quantity;
        this.leavesQuantity = quantity;
        this.state = OrderState.PENDING_NEW;
    }

    public long clientOrderId() {
        return clientOrderId;
    }

    public Long originalClientOrderId() {
        return originalClientOrderId;
    }

    public void originalClientOrderId(Long value) {
        this.originalClientOrderId = value;
    }

    public String submittingBrokerId() {
        return submittingBrokerId;
    }

    public String securityId() {
        return securityId;
    }

    public Side side() {
        return side;
    }

    public OrderType orderType() {
        return orderType;
    }

    public Tif tif() {
        return tif;
    }

    public String exchangeOrderId() {
        return exchangeOrderId;
    }

    public void exchangeOrderId(String value) {
        this.exchangeOrderId = value;
    }

    public long price() {
        return price;
    }

    public void price(long value) {
        this.price = value;
    }

    public long quantity() {
        return quantity;
    }

    public void quantity(long value) {
        this.quantity = value;
    }

    public long cumulativeQuantity() {
        return cumulativeQuantity;
    }

    public long leavesQuantity() {
        return leavesQuantity;
    }

    public OrderState state() {
        return state;
    }

    void state(OrderState value) {
        this.state = value;
    }

    public String lastExecutionId() {
        return lastExecutionId;
    }

    void lastExecutionId(String value) {
        this.lastExecutionId = value;
    }

    // ---- transitions (used only by OrderStateMachine) ----

    void applyFill(long cumQuantity, long leaves, String executionId) {
        this.cumulativeQuantity = cumQuantity;
        this.leavesQuantity = leaves;
        this.lastExecutionId = executionId;
    }

    void applyTradeCancel(long cumQuantity, long leaves, String executionId) {
        this.cumulativeQuantity = cumQuantity;
        this.leavesQuantity = leaves;
        this.lastExecutionId = executionId;
    }

    /** Derived state for a live order from its leaves quantity. */
    OrderState liveState() {
        if (leavesQuantity == 0) {
            return OrderState.FILLED;
        }
        return cumulativeQuantity > 0 ? OrderState.PARTIALLY_FILLED : OrderState.NEW;
    }
}
