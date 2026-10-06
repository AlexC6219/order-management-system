package com.hase.oms.session;

import com.hase.oms.codec.Dictionary;
import com.hase.oms.codec.Message;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Transport-agnostic OCG-C session state machine.
 *
 * <p>Owns sequence tracking, the heartbeat/Test-Request ladder, Logon
 * reconciliation, resend/gap-fill and Logout. Outbound frames are handed to a
 * sink; inbound frames are fed via {@link #onMessage}. Time is read from an
 * injected {@link Clock} so the ladder and timeouts are deterministic in tests.
 *
 * <p>Duplicated/out-of-order inbound handling:
 * <ul>
 *   <li>seq == expected: process and advance.</li>
 *   <li>seq &gt; expected: raise a Resend Request and queue the message.</li>
 *   <li>seq &lt; expected: drop unless PossDup=1, and dedupe by Execution ID.</li>
 * </ul>
 */
public final class SessionEngine {

    private final Dictionary dict;
    private final SessionConfig config;
    private final Clock clock;
    private final Consumer<byte[]> sink;
    private Consumer<Message> inboundHandler;
    private final SequenceTracker sequence = new SequenceTracker();
    private final SessionMessages messages;
    private final List<Message> pending = new ArrayList<>();
    private final List<Long> resendRequestsIssued = new ArrayList<>();

    private SessionState state = SessionState.DISCONNECTED;
    private SessionTermination termination;
    private long lastActivityMillis;
    private long testRequestSentAtMillis = -1;
    private long currentTestRequestId;
    private boolean resendInProgress;
    private long resendFrom;
    private long resendTo;

    /** Bounded, most-recently-seen Execution IDs for PossDup/PossResend dedupe. */
    private final Set<String> seenExecutionIds = new LinkedHashSet<>();
    private static final int DEDUPE_WINDOW = 1024;

    public SessionEngine(Dictionary dict, SessionConfig config, Clock clock, Consumer<byte[]> sink) {
        this.dict = dict;
        this.config = config;
        this.clock = clock;
        this.sink = sink;
        this.messages = new SessionMessages(dict, config.compId());
    }

    public SessionState state() {
        return state;
    }

    public SessionTermination termination() {
        return termination;
    }

    public SequenceTracker sequence() {
        return sequence;
    }

    public List<Message> pendingMessages() {
        return pending;
    }

    public int testRequestCount() {
        return (int) currentTestRequestId;
    }

    /**
     * Registers the handler for inbound business messages (Execution Reports,
     * rejects, mass-cancel reports). Session-management messages are consumed
     * internally and never reach it.
     */
    public void setInboundHandler(Consumer<Message> handler) {
        this.inboundHandler = handler;
    }

    /**
     * Assigns the next outbound sequence number, stamps the Comp ID, and sends a
     * business message through the session. Returns the allocated sequence.
     */
    public long send(Message message) {
        message.sequenceNumber(sequence.allocateOutbound()).compId(config.compId());
        dispatch(message);
        return message.sequenceNumber();
    }

    // ---- lifecycle ----

    /** Marks the TCP connection established and transitions to LOGGING_IN. */
    public void onConnected() {
        state = SessionState.LOGGING_IN;
        lastActivityMillis = clock.millis();
    }

    /** Sends the Logon and records the client's Next Expected for reconciliation. */
    public void sendLogon(String encryptedPassword) {
        state = SessionState.LOGGING_IN;
        Message logon = messages.logon(sequence.allocateOutbound(), encryptedPassword, sequence.nextExpected());
        dispatch(logon);
        lastActivityMillis = clock.millis();
    }

    /** Builds a Logon message without sending it (for inspection/tests). */
    public Message buildLogon(String encryptedPassword) {
        return messages.logon(sequence.nextToSend(), encryptedPassword, sequence.nextExpected());
    }

    /**
     * Handles a Logon reply. Inspects {@code sessionStatus} first: a refused or
     * password-change-required status terminates the session and is never treated
     * as active. On an accepted status the sequence reconciliation is applied.
     *
     * <p>The peer's Next To Send is not carried in the Logon reply itself; the
     * caller supplies it (in production it is the peer sequence from the
     * gateway's session state, in tests it is set explicitly).
     */
    public LogonOutcome onLogonReply(Message reply, long peerNextToSend) {
        long status = number(reply.get("sessionStatus"));
        LogonStatus disposition = LogonStatus.of(status);

        if (disposition == LogonStatus.NEW_PASSWORD_REQUIRED) {
            terminate(SessionTermination.PASSWORD_CHANGE_REQUIRED);
            return new LogonOutcome(false, status, true, null);
        }
        if (!disposition.accepted()) {
            terminate(SessionTermination.LOGON_REJECTED);
            return new LogonOutcome(false, status, false, null);
        }

        LogonReconciliation outcome =
                LogonReconciliation.of(sequence.nextExpected(), peerNextToSend);
        if (outcome == LogonReconciliation.CLIENT_AHEAD) {
            terminate(SessionTermination.SEQUENCE_AHEAD_MANUAL_INTERVENTION);
            return new LogonOutcome(true, status, false, outcome);
        }
        state = SessionState.ACTIVE;
        lastActivityMillis = clock.millis();
        if (outcome == LogonReconciliation.CLIENT_BEHIND) {
            beginResend(sequence.nextExpected(), peerNextToSend - 1);
        }
        return new LogonOutcome(true, status, false, outcome);
    }

    // ---- heartbeat ladder ----

    /** Called on the heartbeat interval; sends a heartbeat when idle. */
    public void onTick() {
        if (state != SessionState.ACTIVE) {
            return;
        }
        long idle = clock.millis() - lastActivityMillis;
        if (idle >= config.logoutAfter().toMillis()) {
            terminate(SessionTermination.HEARTBEAT_TIMEOUT);
            return;
        }
        if (idle >= config.testRequestAfter().toMillis()) {
            // The ladder's Test Request also acts as the delayed heartbeat.
            if (testRequestSentAtMillis < 0) {
                currentTestRequestId = sequence.allocateOutbound();
                Message tr = messages.testRequest(currentTestRequestId, currentTestRequestId);
                dispatch(tr);
                testRequestSentAtMillis = clock.millis();
            }
            return;
        }
        if (idle >= config.heartbeatInterval().toMillis()) {
            dispatch(messages.heartbeat(sequence.allocateOutbound()));
            // resetActivity=false: sending a heartbeat does not by itself prove
            // the peer is alive, so the ladder must keep advancing from the last
            // *inbound* activity.
        }
    }

    /** True once the idle time has passed the Test Request threshold. */
    public boolean testRequestDue() {
        return state == SessionState.ACTIVE
                && (clock.millis() - lastActivityMillis) >= config.testRequestAfter().toMillis();
    }

    // ---- logout ----

    public void sendLogout() {
        state = SessionState.LOGGING_OUT;
        dispatch(messages.logout(sequence.allocateOutbound()));
        lastActivityMillis = clock.millis();
    }

    public void onLogoutReply(Message reply) {
        terminate(SessionTermination.LOGOUT);
    }

    // ---- inbound dispatch ----

    /**
     * Feeds one decoded inbound message. Returns true if the message was
     * processed, false if it was queued, dropped, or consumed as a duplicate.
     */
    public boolean onMessage(Message inbound) {
        int type = inbound.messageType();
        long seq = inbound.sequenceNumber();

        // Reject frames addressed to a different Comp ID when both are known.
        String inboundCompId = inbound.compId();
        if (!inboundCompId.isEmpty() && !config.compId().isEmpty()
                && !inboundCompId.equals(config.compId())) {
            return false;
        }

        // Administrative session traffic bypasses gap checking.
        if (GapFillSkipList.shouldSkip(type) && type != MsgTypes.SEQUENCE_RESET) {
            handleSessionMessage(inbound);
            return true;
        }

        long expected = sequence.nextExpected();
        if (seq == expected) {
            sequence.acceptInbound();
            handleSessionMessage(inbound);
            drainPending();
            return true;
        }
        if (seq > expected) {
            requestResend(expected, seq - 1);
            pending.add(inbound);
            return false;
        }
        // seq < expected: duplicate or resend.
        if (inbound.possDup() == 1) {
            if (isDuplicateExecution(inbound)) {
                return false;
            }
            handleSessionMessage(inbound);
            return true;
        }
        return false;
    }

    private void handleSessionMessage(Message inbound) {
        int type = inbound.messageType();
        if (type == MsgTypes.TEST_REQUEST) {
            long id = number(inbound.get("testRequestId"));
            dispatch(messages.heartbeatReply(sequence.allocateOutbound(), id));
            lastActivityMillis = clock.millis();
            testRequestSentAtMillis = -1;
        } else if (type == MsgTypes.HEARTBEAT) {
            lastActivityMillis = clock.millis();
            testRequestSentAtMillis = -1;
            long ref = number(inbound.get("referenceTestRequestId"));
            if (ref == currentTestRequestId && testRequestSentAtMillis >= 0) {
                testRequestSentAtMillis = -1;
            }
        } else if (type == MsgTypes.LOGON) {
            // The peer's Next To Send is inferred as the sequence immediately
            // after the Logon it just sent.
            onLogonReply(inbound, inbound.sequenceNumber() + 1);
        } else if (type == MsgTypes.LOGOUT) {
            onLogoutReply(inbound);
        } else if (type == MsgTypes.RESEND_REQUEST) {
            if (resendInProgress) {
                // A second Resend Request during a resend terminates the session.
                terminate(SessionTermination.RESEND_IN_PROGRESS);
            } else {
                startResend(inbound);
            }
        } else if (type == MsgTypes.SEQUENCE_RESET) {
            applySequenceReset(inbound);
        }
        if (type == MsgTypes.EXECUTION_REPORT) {
            recordExecution(inbound);
        }
        if (!isSessionManagement(type) && inboundHandler != null) {
            inboundHandler.accept(inbound);
        }
    }

    private static boolean isSessionManagement(int type) {
        return type == MsgTypes.TEST_REQUEST || type == MsgTypes.HEARTBEAT
                || type == MsgTypes.LOGON || type == MsgTypes.LOGOUT
                || type == MsgTypes.RESEND_REQUEST || type == MsgTypes.SEQUENCE_RESET;
    }

    // ---- resend ----

    private void requestResend(long from, long to) {
        resendRequestsIssued.add(from);
        dispatch(messages.resendRequest(sequence.allocateOutbound(), from, to));
    }

    private void beginResend(long from, long to) {
        resendFrom = from;
        resendTo = to;
        resendInProgress = true;
        resendRequestsIssued.add(from);
        dispatch(messages.resendRequest(sequence.allocateOutbound(), from, to));
    }

    private void startResend(Message request) {
        resendFrom = number(request.get("startSequence"));
        resendTo = number(request.get("endSequence"));
        resendInProgress = true;
    }

    private void applySequenceReset(Message reset) {
        boolean gapFill = "Y".equals(reset.get("gapFill"));
        long newSeq = number(reset.get("newSequenceNumber"));
        sequence.setNextExpected(newSeq);
        if (gapFill) {
            resendInProgress = false;
        }
    }

    public boolean isResendInProgress() {
        return resendInProgress;
    }

    public List<Long> resendRequestsIssued() {
        return resendRequestsIssued;
    }

    // ---- pending queue ----

    private void drainPending() {
        boolean progressed = true;
        while (progressed && !pending.isEmpty()) {
            progressed = false;
            long expected = sequence.nextExpected();
            for (int i = 0; i < pending.size(); i++) {
                if (pending.get(i).sequenceNumber() == expected) {
                    Message m = pending.remove(i);
                    sequence.acceptInbound();
                    handleSessionMessage(m);
                    progressed = true;
                    break;
                }
            }
            // Drop queued messages outside the requested range.
            pending.removeIf(m -> m.sequenceNumber() < sequence.nextExpected());
        }
    }

    private boolean isDuplicateExecution(Message m) {
        if (m.messageType() != MsgTypes.EXECUTION_REPORT) {
            return false;
        }
        Object execId = m.get("executionId");
        if (execId == null) {
            return false;
        }
        return seenExecutionIds.contains(String.valueOf(execId));
    }

    private void recordExecution(Message m) {
        Object execId = m.get("executionId");
        if (execId == null) {
            return;
        }
        String id = String.valueOf(execId);
        seenExecutionIds.remove(id);   // re-insert to mark as most recent
        seenExecutionIds.add(id);
        if (seenExecutionIds.size() > DEDUPE_WINDOW) {
            Iterator<String> oldest = seenExecutionIds.iterator();
            oldest.next();
            oldest.remove();
        }
    }

    // ---- internals ----

    private void dispatch(Message message) {
        sink.accept(com.hase.oms.codec.MessageCodec.encode(dict, message));
    }

    private void terminate(SessionTermination reason) {
        if (state == SessionState.TERMINATED) {
            return;
        }
        termination = reason;
        state = SessionState.TERMINATED;
    }

    private static long number(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }
}
