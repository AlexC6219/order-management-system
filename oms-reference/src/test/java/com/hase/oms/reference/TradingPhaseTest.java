package com.hase.oms.reference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradingPhaseTest {

    @Test
    void amendCancelPermittedOutsideBlockingPhases() {
        assertTrue(TradingPhase.CONTINUOUS.permitsAmendOrCancel());
        assertTrue(TradingPhase.PRE_OPENING.permitsAmendOrCancel());
        assertTrue(TradingPhase.CLOSING_AUCTION.permitsAmendOrCancel());
    }

    @Test
    void amendCancelBlockedInNoCancellationRandomMatchingBlocking() {
        assertFalse(TradingPhase.NO_CANCELLATION.permitsAmendOrCancel());
        assertFalse(TradingPhase.RANDOM_MATCHING.permitsAmendOrCancel());
        assertFalse(TradingPhase.BLOCKING.permitsAmendOrCancel());
    }

    @Test
    void auctions() {
        assertTrue(TradingPhase.PRE_OPENING.isAuction());
        assertTrue(TradingPhase.CLOSING_AUCTION.isAuction());
        assertFalse(TradingPhase.CONTINUOUS.isAuction());
    }
}
