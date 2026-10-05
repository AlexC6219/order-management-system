package com.hase.oms.order;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory order repository keyed by Client Order ID and exchange Order ID. */
public final class OrderRepository {

    private final Map<Long, Order> byClientOrderId = new HashMap<>();
    private final Map<String, Order> byExchangeOrderId = new HashMap<>();

    /** Adds an order and rejects a duplicate Client Order ID (FR-4). */
    public void add(Order order) {
        if (byClientOrderId.putIfAbsent(order.clientOrderId(), order) != null) {
            throw new IllegalArgumentException(
                    "Duplicate Client Order ID: " + order.clientOrderId());
        }
    }

    public Optional<Order> byClientOrderId(long clientOrderId) {
        return Optional.ofNullable(byClientOrderId.get(clientOrderId));
    }

    public Optional<Order> byExchangeOrderId(String exchangeOrderId) {
        return exchangeOrderId == null
                ? Optional.empty()
                : Optional.ofNullable(byExchangeOrderId.get(exchangeOrderId));
    }

    /** Records the exchange's Order ID once known. */
    public void indexExchangeOrderId(Order order) {
        if (order.exchangeOrderId() != null) {
            byExchangeOrderId.put(order.exchangeOrderId(), order);
        }
    }

    public boolean containsClientOrderId(long clientOrderId) {
        return byClientOrderId.containsKey(clientOrderId);
    }

    /** Maps a chained amend/cancel Client Order ID back to the same order. */
    public void linkAlias(long clientOrderId, Order order) {
        byClientOrderId.put(clientOrderId, order);
    }

    public int size() {
        return byClientOrderId.size();
    }
}
