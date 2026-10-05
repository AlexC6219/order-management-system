package com.hase.oms.session;

/**
 * Result of processing a Logon reply.
 *
 * @param accepted            true if the peer accepted the logon (status-wise)
 * @param sessionStatus       raw {@code sessionStatus} code from the reply
 * @param passwordChangeRequired true when the peer demands a new password
 * @param reconciliation      sequence reconciliation, or {@code null} if the
 *                            logon was refused
 */
public record LogonOutcome(boolean accepted, long sessionStatus,
                           boolean passwordChangeRequired,
                           LogonReconciliation reconciliation) {
}
