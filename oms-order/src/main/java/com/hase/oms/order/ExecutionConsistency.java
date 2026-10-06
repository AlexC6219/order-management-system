package com.hase.oms.order;

/**
 * Asserts {@code leavesQuantity = orderQuantity - cumulativeQuantity} on fills
 * (agent-added ALE-69). A mismatch means venue/decoder disagreement; the order
 * must not be silently advanced.
 */
public final class ExecutionConsistency {

    private ExecutionConsistency() {}

    public static void checkFill(Order order, long cumulativeQuantity, long leavesQuantity) {
        long expectedLeaves = order.quantity() - cumulativeQuantity;
        if (leavesQuantity != expectedLeaves) {
            throw new InconsistentExecutionException(
                    "leaves=" + leavesQuantity + " expected=" + expectedLeaves
                            + " for order " + order.clientOrderId());
        }
    }
}
