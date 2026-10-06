package com.hase.oms.order;

/**
 * Internal order lifecycle (DESIGN.md §8.1).
 *
 * <pre>
 *  PENDING_NEW ──ack──▶ NEW ──fill──▶ PARTIALLY_FILLED ──▶ FILLED
 *      │                │  │                │
 *    reject          amend│ │cancel        full
 *      ▼                ▼  ▼
 *  REJECTED     PENDING_REPLACE  PENDING_CANCEL
 * </pre>
 */
public enum OrderState {
    PENDING_NEW,
    NEW,
    PARTIALLY_FILLED,
    FILLED,
    PENDING_REPLACE,
    PENDING_CANCEL,
    REJECTED,
    CANCELLED,
    EXPIRED;

    /** Terminal states admit no further transitions. */
    public boolean isTerminal() {
        return this == FILLED || this == REJECTED || this == CANCELLED || this == EXPIRED;
    }

    /** True while an amend/cancel is awaiting the venue's reply. */
    public boolean isPending() {
        return this == PENDING_NEW || this == PENDING_REPLACE || this == PENDING_CANCEL;
    }
}
