package com.hase.oms.order;

import com.hase.oms.codec.Message;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Orchestrates order ingress, OCG-C translation and Execution Report
 * reconciliation (DESIGN.md §8; FR-1/4/7/8).
 *
 * <p>Outbound OCG-C messages are handed to a sink (typically
 * {@code SessionEngine::send}), which assigns the session sequence number. This
 * class stays independent of the session module.
 */
public final class OrderManager {

    private final ClientOrderIdAllocator allocator;
    private final OrderRepository repository;
    private final OrderStateMachine stateMachine;
    private final OrderTranslator translator;
    private final Consumer<Message> outbound;

    private final Set<String> seenUpstreamClOrdIds = new HashSet<>();
    private long lastMassCancelId;

    public OrderManager(ClientOrderIdAllocator allocator, OrderTranslator translator,
                        Consumer<Message> outbound) {
        this.allocator = allocator;
        this.translator = translator;
        this.outbound = outbound;
        this.repository = new OrderRepository();
        this.stateMachine = new OrderStateMachine();
    }

    public OrderRepository repository() {
        return repository;
    }

    /** Accepts a new order, allocating the OCG-C Client Order ID (FR-4). */
    public Order submit(OrderRequest request) {
        if (request.upstreamClOrdId() != null && !seenUpstreamClOrdIds.add(request.upstreamClOrdId())) {
            throw new IllegalArgumentException("Duplicate upstream ClOrdID: " + request.upstreamClOrdId());
        }
        IngressValidator.validateText(request.text());
        IngressValidator.validateNoLeadingZeros(request.securityId());
        IngressValidator.validateNoLeadingZeros(request.submittingBrokerId());

        long clientOrderId = allocator.allocate();
        Order order = new Order(clientOrderId, request.submittingBrokerId(), request.securityId(),
                request.side(), request.orderType(), request.tif(),
                request.price(), request.quantity());
        repository.add(order);
        outbound.accept(translator.newOrder(order, 0L));
        return order;
    }

    /** Requests an amend; consumes a fresh Client Order ID and links the chain. */
    public long amend(Order order, long newPrice, long newQuantity) {
        if (!stateMachine.requestAmend(order)) {
            throw new IllegalStateException("Cannot amend order in state " + order.state());
        }
        long newClientOrderId = allocator.allocate();
        repository.linkAlias(newClientOrderId, order);
        order.price(newPrice);
        order.quantity(newQuantity);
        outbound.accept(translator.amend(order, newClientOrderId, order.clientOrderId(), 0L));
        return newClientOrderId;
    }

    /** Requests a cancel; consumes a fresh Client Order ID and links the chain. */
    public long cancel(Order order) {
        if (!stateMachine.requestCancel(order)) {
            throw new IllegalStateException("Cannot cancel order in state " + order.state());
        }
        long newClientOrderId = allocator.allocate();
        repository.linkAlias(newClientOrderId, order);
        outbound.accept(translator.cancel(order, newClientOrderId, order.clientOrderId(), 0L));
        return newClientOrderId;
    }

    public long massCancel(String submittingBrokerId) {
        lastMassCancelId = allocator.allocate();
        outbound.accept(translator.massCancel(lastMassCancelId, submittingBrokerId, 0L));
        return lastMassCancelId;
    }

    /** Reconciles an inbound OCG-C Execution Report against the order. */
    public boolean onExecutionReport(Message executionReport) {
        Object clientOrderIdValue = executionReport.get("clientOrderId");
        if (clientOrderIdValue == null) {
            return false;
        }
        long clientOrderId = Long.parseLong(String.valueOf(clientOrderIdValue));
        Order order = repository.byClientOrderId(clientOrderId).orElse(null);
        if (order == null) {
            return false;
        }

        String executionId = (String) executionReport.get("executionId");
        if (executionId != null && executionId.equals(order.lastExecutionId())) {
            return false; // duplicate execution (PossResend)
        }

        OrderEvent event = translator.decodeExecutionReport(executionReport);
        if (event instanceof OrderEvent.Fill fill) {
            ExecutionConsistency.checkFill(order, fill.cumulativeQuantity(), fill.leavesQuantity());
        }
        boolean applied = stateMachine.apply(order, event);
        if (applied) {
            repository.indexExchangeOrderId(order);
        }
        return applied;
    }
}
