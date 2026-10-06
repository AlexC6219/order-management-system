package com.hase.oms.order;

import com.hase.oms.codec.Message;

import java.time.Clock;

/**
 * Builds OCG-C order messages from internal orders (DESIGN.md §8).
 *
 * <p>Field names/types follow the OCG-C dictionary; the mapping is fixed by
 * DESIGN.md §8. Optional repeating (`multi`) fields are deliberately omitted in
 * v1.
 */
public final class OrderTranslator {

    private static final int NEW_ORDER = 11;
    private static final int AMEND_REQUEST = 12;
    private static final int CANCEL_REQUEST = 13;
    private static final int MASS_CANCEL_REQUEST = 14;

    private static final long SECURITY_ID_SOURCE_EXCHANGE_SYMBOL = 8L;
    private static final long DISCLOSURE_INSTRUCTIONS_NONE = 0L;
    private static final long MASS_CANCEL_ALL_ORDERS = 7L;

    private final String compId;
    private final Clock clock;

    public OrderTranslator(String compId, Clock clock) {
        this.compId = compId;
        this.clock = clock;
    }

    public Message newOrder(Order order, long seq) {
        IngressValidator.validateClientOrderId(order.clientOrderId());
        IngressValidator.validateNoLeadingZeros(order.securityId());
        IngressValidator.validateNoLeadingZeros(order.submittingBrokerId());
        IngressValidator.validateSide(order.side().code());
        IngressValidator.validateOrderType(order.orderType().code());
        IngressValidator.validateTif(order.tif().code());

        return header(NEW_ORDER, seq)
                .put("clientOrderId", Long.toString(order.clientOrderId()))
                .put("submittingBrokerId", order.submittingBrokerId())
                .put("securityId", order.securityId())
                .put("securityIdSource", SECURITY_ID_SOURCE_EXCHANGE_SYMBOL)
                .put("securityExchange", "XHKG")
                .put("transactionTime", TransactionTime.now(clock))
                .put("side", order.side().code())
                .put("orderType", order.orderType().code())
                .put("price", order.price())
                .put("orderQuantity", order.quantity())
                .put("tif", order.tif().code())
                .put("disclosureInstructions", DISCLOSURE_INSTRUCTIONS_NONE);
    }

    public Message amend(Order order, long newClientOrderId, long originalClientOrderId, long seq) {
        return header(AMEND_REQUEST, seq)
                .put("clientOrderId", Long.toString(newClientOrderId))
                .put("submittingBrokerId", order.submittingBrokerId())
                .put("securityId", order.securityId())
                .put("securityIdSource", SECURITY_ID_SOURCE_EXCHANGE_SYMBOL)
                .put("securityExchange", "XHKG")
                .put("transactionTime", TransactionTime.now(clock))
                .put("side", order.side().code())
                .put("originalClientOrderId", Long.toString(originalClientOrderId))
                .put("orderType", order.orderType().code())
                .put("price", order.price())
                .put("orderQuantity", order.quantity())
                .put("tif", order.tif().code())
                .put("disclosureInstructions", DISCLOSURE_INSTRUCTIONS_NONE);
    }

    public Message cancel(Order order, long newClientOrderId, long originalClientOrderId, long seq) {
        return header(CANCEL_REQUEST, seq)
                .put("clientOrderId", Long.toString(newClientOrderId))
                .put("submittingBrokerId", order.submittingBrokerId())
                .put("securityId", order.securityId())
                .put("securityIdSource", SECURITY_ID_SOURCE_EXCHANGE_SYMBOL)
                .put("securityExchange", "XHKG")
                .put("transactionTime", TransactionTime.now(clock))
                .put("side", order.side().code())
                .put("originalClientOrderId", Long.toString(originalClientOrderId));
    }

    public Message massCancel(long clientOrderId, String submittingBrokerId, long seq) {
        return header(MASS_CANCEL_REQUEST, seq)
                .put("clientOrderId", Long.toString(clientOrderId))
                .put("submittingBrokerId", submittingBrokerId)
                .put("transactionTime", TransactionTime.now(clock))
                .put("massCancelRequestType", MASS_CANCEL_ALL_ORDERS);
    }

    /** Decodes an OCG-C Execution Report into an internal event. */
    public OrderEvent decodeExecutionReport(Message executionReport) {
        return ExecTypeMapper.toEvent(executionReport);
    }

    private Message header(int type, long seq) {
        return new Message(type).sequenceNumber(seq).compId(compId);
    }
}
