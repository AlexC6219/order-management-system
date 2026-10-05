package com.hase.oms.fix;

/** FIX 5.0 SP2 tag numbers used by the mapping (subset). */
public final class FixTags {

    // header
    public static final int BEGIN_STRING = 8;
    public static final int MSG_TYPE = 35;
    public static final int SENDER_COMP_ID = 49;
    public static final int TARGET_COMP_ID = 56;
    public static final int MSG_SEQ_NUM = 34;
    public static final int SENDING_TIME = 52;

    // common
    public static final int ACCOUNT = 1;
    public static final int AVG_PX = 6;
    public static final int CL_ORD_ID = 11;
    public static final int CUM_QTY = 14;
    public static final int EXEC_ID = 17;
    public static final int SECURITY_ID_SOURCE = 22;
    public static final int LAST_QTY = 32;
    public static final int LAST_PX = 31;
    public static final int ORDER_ID = 37;
    public static final int ORDER_QTY = 38;
    public static final int ORD_STATUS = 39;
    public static final int ORD_TYPE = 40;
    public static final int ORIG_CL_ORD_ID = 41;
    public static final int PRICE = 44;
    public static final int SIDE = 54;
    public static final int SYMBOL = 55;
    public static final int TEXT = 58;
    public static final int TIME_IN_FORCE = 59;
    public static final int TRANSACT_TIME = 60;
    public static final int SECURITY_EXCHANGE = 207;
    public static final int EXEC_TYPE = 150;
    public static final int LEAVES_QTY = 151;
    public static final int ORD_REJ_REASON = 103;
    public static final int CXL_REJ_REASON = 102;
    public static final int CXL_REJ_RESPONSE_TO = 434;

    // MsgType values
    public static final String EXECUTION_REPORT = "8";
    public static final String ORDER_CANCEL_REJECT = "9";
    public static final String NEW_ORDER_SINGLE = "D";
    public static final String ORDER_CANCEL_REQUEST = "F";
    public static final String ORDER_CANCEL_REPLACE_REQUEST = "G";

    private FixTags() {}
}
