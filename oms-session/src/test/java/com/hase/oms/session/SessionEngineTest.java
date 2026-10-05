package com.hase.oms.session;

import com.hase.oms.codec.Dictionary;
import com.hase.oms.codec.Message;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** L2 session state-machine tests driven directly (no sockets). */
class SessionEngineTest {

    static Dictionary dict() {
        InputStream fields = SessionEngineTest.class.getResourceAsStream("/fields.yaml");
        InputStream messages = SessionEngineTest.class.getResourceAsStream("/messages.yaml");
        return Dictionary.load(fields, messages);
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advanceMillis(long millis) {
            now = now.plusMillis(millis);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static SessionEngine engine(MutableClock clock, List<byte[]> out) {
        SessionConfig config = new SessionConfig().compId("TEST");
        return new SessionEngine(dict(), config, clock, out::add);
    }

    @Test
    void sequenceTrackerStartsAtOne() {
        SequenceTracker tracker = new SequenceTracker();
        assertEquals(1L, tracker.nextToSend());
        assertEquals(1L, tracker.nextExpected());
        assertEquals(1L, tracker.allocateOutbound());
        assertEquals(2L, tracker.nextToSend());
    }

    @Test
    void reconciliationBranches() {
        assertEquals(LogonReconciliation.CLIENT_AHEAD, LogonReconciliation.of(11, 10));
        assertEquals(LogonReconciliation.IN_SYNC, LogonReconciliation.of(10, 10));
        assertEquals(LogonReconciliation.CLIENT_BEHIND, LogonReconciliation.of(9, 10));
    }

    @Test
    void logonInSyncBecomesActive() {
        MutableClock clock = new MutableClock();
        List<byte[]> out = new ArrayList<>();
        SessionEngine engine = engine(clock, out);

        engine.onConnected();
        engine.sendLogon("cipher");
        assertEquals(SessionState.LOGGING_IN, engine.state());

        Message reply = new Message(MsgTypes.LOGON).sequenceNumber(0).put("sessionStatus", 0L);
        LogonReconciliation outcome = engine.onLogonReply(reply, 1);

        assertEquals(LogonReconciliation.IN_SYNC, outcome);
        assertEquals(SessionState.ACTIVE, engine.state());
    }

    @Test
    void logonClientAheadTerminates() {
        MutableClock clock = new MutableClock();
        List<byte[]> out = new ArrayList<>();
        SessionEngine engine = engine(clock, out);
        engine.sequence().setNextExpected(50);

        engine.onConnected();
        Message reply = new Message(MsgTypes.LOGON).sequenceNumber(0).put("sessionStatus", 0L);
        LogonReconciliation outcome = engine.onLogonReply(reply, 1);

        assertEquals(LogonReconciliation.CLIENT_AHEAD, outcome);
        assertEquals(SessionState.TERMINATED, engine.state());
        assertEquals(SessionTermination.SEQUENCE_AHEAD_MANUAL_INTERVENTION, engine.termination());
    }

    @Test
    void logonClientBehindStartsResend() {
        MutableClock clock = new MutableClock();
        List<byte[]> out = new ArrayList<>();
        SessionEngine engine = engine(clock, out);
        engine.sequence().setNextExpected(5);

        engine.onConnected();
        Message reply = new Message(MsgTypes.LOGON).sequenceNumber(0).put("sessionStatus", 0L);
        LogonReconciliation outcome = engine.onLogonReply(reply, 10);

        assertEquals(LogonReconciliation.CLIENT_BEHIND, outcome);
        assertTrue(engine.isResendInProgress());
        assertEquals(5L, engine.resendRequestsIssued().get(0));
    }

    @Test
    void heartbeatOnIdleThenTestRequestThenLogout() {
        MutableClock clock = new MutableClock();
        List<byte[]> out = new ArrayList<>();
        SessionConfig config = new SessionConfig().compId("TEST");
        SessionEngine engine = new SessionEngine(dict(), config, clock, out::add);

        engine.onConnected();
        engine.onLogonReply(new Message(MsgTypes.LOGON).sequenceNumber(0), 1);

        int before = out.size();
        clock.advanceMillis(20_000);
        engine.onTick();
        assertEquals(before + 1, out.size(), "one heartbeat after 20s idle");

        // Advance past 3 intervals -> Test Request instead of heartbeat.
        clock.advanceMillis(40_000);
        engine.onTick();
        assertTrue(engine.testRequestCount() > 0);

        // Advance past the full ladder -> Logout/terminate.
        clock.advanceMillis(config.logoutAfter().toMillis());
        engine.onTick();
        assertEquals(SessionState.TERMINATED, engine.state());
        assertEquals(SessionTermination.HEARTBEAT_TIMEOUT, engine.termination());
    }

    @Test
    void gapRaisesResendAndQueuesOutOfOrder() {
        MutableClock clock = new MutableClock();
        List<byte[]> out = new ArrayList<>();
        SessionEngine engine = engine(clock, out);
        engine.onConnected();
        engine.onLogonReply(new Message(MsgTypes.LOGON).sequenceNumber(0), 1);

        // Expect 1, receive 3 -> resend request + queued.
        Message three = new Message(MsgTypes.EXECUTION_REPORT).sequenceNumber(3).put("executionId", "E3");
        assertFalse(engine.onMessage(three));
        assertEquals(1L, engine.resendRequestsIssued().get(0));
        assertEquals(1, engine.pendingMessages().size());
    }

    @Test
    void queuedMessageDrainsWhenGapFilled() {
        MutableClock clock = new MutableClock();
        List<byte[]> out = new ArrayList<>();
        SessionEngine engine = engine(clock, out);
        engine.onConnected();
        engine.onLogonReply(new Message(MsgTypes.LOGON).sequenceNumber(0), 1);

        engine.onMessage(new Message(MsgTypes.EXECUTION_REPORT).sequenceNumber(3).put("executionId", "E3"));
        engine.onMessage(new Message(MsgTypes.EXECUTION_REPORT).sequenceNumber(1).put("executionId", "E1"));
        engine.onMessage(new Message(MsgTypes.EXECUTION_REPORT).sequenceNumber(2).put("executionId", "E2"));

        assertTrue(engine.pendingMessages().isEmpty());
        assertEquals(4L, engine.sequence().nextExpected());
    }

    @Test
    void duplicatePossDupExecutionDeduped() {
        MutableClock clock = new MutableClock();
        List<byte[]> out = new ArrayList<>();
        SessionEngine engine = engine(clock, out);
        engine.onConnected();
        engine.onLogonReply(new Message(MsgTypes.LOGON).sequenceNumber(0), 1);

        Message exec = new Message(MsgTypes.EXECUTION_REPORT).sequenceNumber(1).put("executionId", "E1");
        assertTrue(engine.onMessage(exec));
        // Same execution id, older seq, PossDup -> dropped as duplicate.
        Message dup = new Message(MsgTypes.EXECUTION_REPORT).sequenceNumber(1).possDup(1).put("executionId", "E1");
        assertFalse(engine.onMessage(dup));
    }

    @Test
    void sequenceResetGapFillAdvances() {
        MutableClock clock = new MutableClock();
        List<byte[]> out = new ArrayList<>();
        SessionEngine engine = engine(clock, out);
        engine.onConnected();
        engine.onLogonReply(new Message(MsgTypes.LOGON).sequenceNumber(0), 1);

        Message reset = new Message(MsgTypes.SEQUENCE_RESET).sequenceNumber(1)
                .put("gapFill", "Y").put("newSequenceNumber", 100L);
        engine.onMessage(reset);
        assertEquals(100L, engine.sequence().nextExpected());
    }

    @Test
    void secondResendDuringResendTerminates() {
        MutableClock clock = new MutableClock();
        List<byte[]> out = new ArrayList<>();
        SessionEngine engine = engine(clock, out);
        engine.sequence().setNextExpected(5);
        engine.onConnected();
        engine.onLogonReply(new Message(MsgTypes.LOGON).sequenceNumber(0), 10);
        assertTrue(engine.isResendInProgress());

        Message second = new Message(MsgTypes.RESEND_REQUEST).sequenceNumber(1)
                .put("startSequence", 1L).put("endSequence", 4L);
        engine.onMessage(second);
        assertEquals(SessionState.TERMINATED, engine.state());
        assertEquals(SessionTermination.RESEND_IN_PROGRESS, engine.termination());
    }

    @Test
    void logoutRequestThenReply() {
        MutableClock clock = new MutableClock();
        List<byte[]> out = new ArrayList<>();
        SessionEngine engine = engine(clock, out);
        engine.onConnected();
        engine.onLogonReply(new Message(MsgTypes.LOGON).sequenceNumber(0), 1);

        engine.sendLogout();
        assertEquals(SessionState.LOGGING_OUT, engine.state());
        engine.onLogoutReply(new Message(MsgTypes.LOGOUT).sequenceNumber(1));
        assertEquals(SessionState.TERMINATED, engine.state());
        assertEquals(SessionTermination.LOGOUT, engine.termination());
    }

    @Test
    void gapFillSkipListIncludesSessionMessages() {
        assertTrue(GapFillSkipList.shouldSkip(MsgTypes.LOGON));
        assertTrue(GapFillSkipList.shouldSkip(MsgTypes.HEARTBEAT));
        assertFalse(GapFillSkipList.shouldSkip(MsgTypes.EXECUTION_REPORT));
    }
}
