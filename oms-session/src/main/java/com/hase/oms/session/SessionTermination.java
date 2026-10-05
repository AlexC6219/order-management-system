package com.hase.oms.session;

/** Why a session terminated, surfaced for monitoring and failover (§7.2). */
public enum SessionTermination {

    /** Peer refused the Logon (bad credentials, locked, expired, not allowed). */
    LOGON_REJECTED,

    /** Peer demands a new password before the session can become active. */
    PASSWORD_CHANGE_REQUIRED,

    /** Client's Next Expected was greater than the peer's Next To Send. */
    SEQUENCE_AHEAD_MANUAL_INTERVENTION,

    /** Peer or client logged out cleanly. */
    LOGOUT,

    /** No reply after the heartbeat ladder expired. */
    HEARTBEAT_TIMEOUT,

    /** A second Resend Request arrived while a resend was in progress. */
    RESEND_IN_PROGRESS,

    /** Requested by the owner (shutdown). */
    SHUTDOWN
}
