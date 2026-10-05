package com.hase.oms.harness;

/**
 * OCG-C message type codes used by the mock harness. Kept local so the harness
 * depends only on {@code oms-codec}, not on {@code oms-session}.
 */
public final class MsgCodes {

    public static final int HEARTBEAT = 0;
    public static final int TEST_REQUEST = 1;
    public static final int RESEND_REQUEST = 2;
    public static final int SEQUENCE_RESET = 4;
    public static final int LOGON = 5;
    public static final int LOGOUT = 6;
    public static final int LOOKUP_REQUEST = 7;
    public static final int LOOKUP_RESPONSE = 8;
    public static final int EXECUTION_REPORT = 10;
    public static final int NEW_ORDER = 11;
    public static final int AMEND_REQUEST = 12;
    public static final int CANCEL_REQUEST = 13;
    public static final int MASS_CANCEL_REQUEST = 14;
    public static final int ORDER_MASS_CANCEL_REPORT = 15;

    private MsgCodes() {}
}
