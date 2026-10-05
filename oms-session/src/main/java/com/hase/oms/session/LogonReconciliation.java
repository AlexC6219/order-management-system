package com.hase.oms.session;

/**
 * Outcome of comparing the client's {@code Next Expected Message Sequence}
 * against the peer's Next To Send, per DESIGN.md §7.2.
 */
public enum LogonReconciliation {

    /** N > S: peer is missing messages we never sent; session must terminate. */
    CLIENT_AHEAD,

    /** N == S: clean resume from S. */
    IN_SYNC,

    /** N < S: peer will gap-fill from N up to the logon sequence. */
    CLIENT_BEHIND;

    /**
     * Compares the client's next-expected value {@code n} against the peer's
     * next-to-send value {@code s}.
     */
    public static LogonReconciliation of(long n, long s) {
        if (n > s) {
            return CLIENT_AHEAD;
        }
        if (n < s) {
            return CLIENT_BEHIND;
        }
        return IN_SYNC;
    }
}
