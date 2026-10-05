package com.hase.oms.session;

import com.hase.oms.codec.Dictionary;
import com.hase.oms.codec.Message;
import com.hase.oms.codec.MessageCodec;
import com.hase.oms.session.mock.MockOcgServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * L2 end-to-end: a real socket connection drives the Mock OCG-C server through
 * Lookup, Logon, a scripted Execution Report gap-fill, and Logout.
 */
class MockOcgSessionTest {

    private Dictionary dict;
    private MockOcgServer server;

    @BeforeEach
    void setUp() throws IOException {
        dict = SessionEngineTest.dict();
        server = new MockOcgServer(dict);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private void writeFrame(DataOutputStream out, Message m) throws IOException {
        byte[] frame = MessageCodec.encode(dict, m);
        out.writeInt(frame.length);
        out.write(frame);
        out.flush();
    }

    private Message readFrame(DataInputStream in) throws IOException {
        int len = in.readInt();
        byte[] frame = new byte[len];
        in.readFully(frame);
        return MessageCodec.decode(dict, frame);
    }

    @Test
    void logonHandshakeOverTcp() throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(server.host(), server.port()));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // Lookup with sequence 1.
            SessionMessages messages = new SessionMessages(dict, "TEST");
            writeFrame(out, messages.lookupRequest(1, 1, 1));
            Message lookup = readFrame(in);
            assertEquals(MsgTypes.LOOKUP_RESPONSE, lookup.messageType());

            // Logon with RSA-encrypted password.
            PasswordCipher cipher = new PasswordCipher(server.keyPair().getPublic());
            writeFrame(out, messages.logon(2, cipher.encrypt("Password1"), 1));
            Message logonReply = readFrame(in);
            assertEquals(MsgTypes.LOGON, logonReply.messageType());
            assertEquals(0L, ((Number) logonReply.get("sessionStatus")).longValue());
        }
    }

    @Test
    void logonRejectedWithWrongPassword() throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(server.host(), server.port()));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            SessionMessages messages = new SessionMessages(dict, "TEST");
            PasswordCipher cipher = new PasswordCipher(server.keyPair().getPublic());
            writeFrame(out, messages.logon(1, cipher.encrypt("WrongPass"), 1));

            Message reply = readFrame(in);
            assertEquals(5L, ((Number) reply.get("sessionStatus")).longValue());
        }
    }

    @Test
    void resendRequestReplaysExecutionReports() throws Exception {
        server.setAdvertisedNextToSend(5);
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(server.host(), server.port()));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            SessionMessages messages = new SessionMessages(dict, "TEST");
            writeFrame(out, messages.resendRequest(1, 2, 4));

            List<Message> replayed = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                replayed.add(readFrame(in));
            }
            assertEquals(3, replayed.size());
            assertEquals(MsgTypes.EXECUTION_REPORT, replayed.get(0).messageType());
            assertEquals(2L, replayed.get(0).sequenceNumber());
            assertEquals(4L, replayed.get(2).sequenceNumber());
        }
    }

    @Test
    void engineReachesActiveAgainstMock() throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(server.host(), server.port()));
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            List<byte[]> sent = new ArrayList<>();
            SessionConfig config = new SessionConfig().compId("TEST");
            SessionEngine engine = new SessionEngine(dict, config, Clock.systemUTC(), sent::add);
            engine.onConnected();

            PasswordCipher cipher = new PasswordCipher(server.keyPair().getPublic());
            engine.sendLogon(cipher.encrypt("Password1"));
            for (byte[] frame : sent) {
                out.writeInt(frame.length);
                out.write(frame);
            }
            out.flush();
            sent.clear();

            Message reply = readFrame(in);
            LogonReconciliation outcome = engine.onLogonReply(reply, 1);

            assertEquals(LogonReconciliation.IN_SYNC, outcome);
            assertEquals(SessionState.ACTIVE, engine.state());
            assertTrue(server.received().stream().anyMatch(m -> m.messageType() == MsgTypes.LOGON));
        }
    }
}
