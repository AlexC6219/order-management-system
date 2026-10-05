package com.hase.oms.session;

/**
 * OCG-C message type codes used by the session layer.
 *
 * <p>Mirrors {@code MsgType} in the generated enums but kept here as narrow
 * constants so the session state machine does not depend on the whole enum.
 */
public final class MsgTypes {

    public static final int HEARTBEAT = 0;
    public static final int TEST_REQUEST = 1;
    public static final int RESEND_REQUEST = 2;
    public static final int REJECT = 3;
    public static final int SEQUENCE_RESET = 4;
    public static final int LOGON = 5;
    public static final int LOGOUT = 6;
    public static final int LOOKUP_REQUEST = 7;
    public static final int LOOKUP_RESPONSE = 8;
    public static final int BUSINESS_MESSAGE_REJECT = 9;
    public static final int EXECUTION_REPORT = 10;

    private MsgTypes() {}
}
