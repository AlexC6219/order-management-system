package com.hase.oms.session;

import java.util.Set;

/**
 * Message types that are never replayed by gap-fill (DESIGN.md §7.3). They are
 * administrative session messages that carry no order state.
 */
public final class GapFillSkipList {

    private static final Set<Integer> SKIP = Set.of(
            MsgTypes.LOGON,
            MsgTypes.LOGOUT,
            MsgTypes.HEARTBEAT,
            MsgTypes.TEST_REQUEST,
            MsgTypes.RESEND_REQUEST,
            MsgTypes.SEQUENCE_RESET);

    private GapFillSkipList() {}

    public static boolean shouldSkip(int messageType) {
        return SKIP.contains(messageType);
    }
}
