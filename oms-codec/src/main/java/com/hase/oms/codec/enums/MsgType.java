package com.hase.oms.codec.enums;

/** OCG-C message types (HKEX spec 7.1). */
public enum MsgType {
    Heartbeat(0),
    TestRequest(1),
    ResendRequest(2),
    Reject(3),
    SequenceReset(4),
    Logon(5),
    Logout(6),
    LookupRequest(7),
    LookupResponse(8),
    BusinessMessageReject(9),
    ExecutionReport(10),
    NewOrder(11),
    AmendRequest(12),
    CancelRequest(13),
    MassCancelRequest(14),
    OrderMassCancelReport(15),
    Quote(16),
    QuoteCancel(17),
    QuoteStatusReport(18),
    TradeCaptureReport(21),
    TradeCaptureReportAck(22),
    ObOCancelRequest(23),
    ObOMassCancelRequest(24),
    ThrottleEntitlementRequest(25),
    ThrottleEntitlementResponse(26),
    PartyEntitlementsRequest(27),
    PartyEntitlementsReport(28);

    private final int code;

    MsgType(int code) { this.code = code; }

    public int code() { return code; }

    public static MsgType fromCode(int code) {
        for (MsgType t : values()) {
            if (t.code == code) return t;
        }
        throw new IllegalArgumentException("Unknown MsgType: " + code);
    }
}
