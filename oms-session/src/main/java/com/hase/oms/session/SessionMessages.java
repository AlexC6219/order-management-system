package com.hase.oms.session;

import com.hase.oms.codec.Dictionary;
import com.hase.oms.codec.Message;

/**
 * Builds OCG-C session messages with the {@link Dictionary} as the source of
 * truth for field names. Every session message carries the configured Comp ID.
 */
public final class SessionMessages {

    private final Dictionary dict;
    private final String compId;

    public SessionMessages(Dictionary dict, String compId) {
        this.dict = dict;
        this.compId = compId;
    }

    public Dictionary dictionary() {
        return dict;
    }

    public Message heartbeat(long seq) {
        return header(MsgTypes.HEARTBEAT, seq);
    }

    public Message heartbeatReply(long seq, long testRequestId) {
        return header(MsgTypes.HEARTBEAT, seq).put("referenceTestRequestId", testRequestId);
    }

    public Message testRequest(long seq, long testRequestId) {
        return header(MsgTypes.TEST_REQUEST, seq).put("testRequestId", testRequestId);
    }

    public Message resendRequest(long seq, long start, long end) {
        return header(MsgTypes.RESEND_REQUEST, seq)
                .put("startSequence", start)
                .put("endSequence", end);
    }

    public Message sequenceReset(long seq, long newSeq, boolean gapFill) {
        return header(MsgTypes.SEQUENCE_RESET, seq)
                .put("gapFill", gapFill ? "Y" : "N")
                .put("newSequenceNumber", newSeq);
    }

    public Message logon(long seq, String encryptedPassword, long nextExpected) {
        return header(MsgTypes.LOGON, seq)
                .put("password", encryptedPassword)
                .put("nextExpectedMessageSequence", nextExpected);
    }

    public Message logout(long seq) {
        return header(MsgTypes.LOGOUT, seq);
    }

    public Message lookupRequest(long seq, int typeOfService, int protocolType) {
        return header(MsgTypes.LOOKUP_REQUEST, seq)
                .put("typeOfService", (long) typeOfService)
                .put("protocolType", (long) protocolType);
    }

    private Message header(int type, long seq) {
        return new Message(type).sequenceNumber(seq).compId(compId);
    }
}
