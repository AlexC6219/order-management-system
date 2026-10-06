package com.hase.oms.reference;

/**
 * Trading session phase (DESIGN.md §5, UR.md §5). Market-wide; the price check
 * keys its rule selection off this.
 */
public enum TradingPhase {
    /** Pre-Opening Session. */
    PRE_OPENING,
    /** Closing Auction Session. */
    CLOSING_AUCTION,
    /** Continuous Trading Session. */
    CONTINUOUS,
    /** No-cancellation window: amend/cancel not permitted. */
    NO_CANCELLATION,
    /** Random Matching: amend/cancel not permitted. */
    RANDOM_MATCHING,
    /** Blocking: amend/cancel not permitted. */
    BLOCKING,
    /** No active trading. */
    CLOSED;

    /** True when amend/cancel requests are permitted in this phase. */
    public boolean permitsAmendOrCancel() {
        return this != NO_CANCELLATION && this != RANDOM_MATCHING && this != BLOCKING;
    }

    public boolean isAuction() {
        return this == PRE_OPENING || this == CLOSING_AUCTION;
    }
}
