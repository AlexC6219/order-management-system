package com.hase.oms.session;

/** Lifecycle states of the OCG-C session (DESIGN.md §7.1). */
public enum SessionState {
    DISCONNECTED,
    CONNECTING,
    LOGGING_IN,
    ACTIVE,
    LOGGING_OUT,
    TERMINATED
}
