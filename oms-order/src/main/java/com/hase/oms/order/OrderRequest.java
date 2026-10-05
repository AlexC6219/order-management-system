package com.hase.oms.order;

import com.hase.oms.codec.enums.OrderType;
import com.hase.oms.codec.enums.Side;
import com.hase.oms.codec.enums.Tif;

/**
 * A new-order request translated from the upstream FIX boundary.
 *
 * <p>{@code upstreamClOrdId} is the FIX {@code ClOrdID}; it is used only for
 * ingress dedupe — the OCG-C Client Order ID is allocated by the OMS (FR-4).
 */
public record OrderRequest(String upstreamClOrdId, String submittingBrokerId, String securityId,
                           Side side, OrderType orderType, Tif tif,
                           long price, long quantity, String text) {
}
