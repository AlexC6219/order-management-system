package com.hase.oms.fix;

import com.hase.oms.order.OrderRequest;

/** Callbacks from the FIX acceptor into the OMS order flow. */
public interface FixOrderHandler {

    void onNewOrder(OrderRequest request);

    void onCancel(long originalClientOrderId);

    void onCancelReplace(long originalClientOrderId, OrderRequest replacement);
}
