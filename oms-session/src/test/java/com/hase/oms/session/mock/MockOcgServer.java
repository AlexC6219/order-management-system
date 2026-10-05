package com.hase.oms.session.mock;

import com.hase.oms.codec.Dictionary;
import com.hase.oms.codec.Message;
import com.hase.oms.codec.MessageCodec;
import com.hase.oms.session.MsgTypes;
import com.hase.oms.session.PasswordCipher;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-house Mock OCG-C server (TESTING.md §1.1).
 *
 * <p>Speaks the codegen'd OCG-C binary protocol over TCP, answers Lookup,
 * validates Logon with a self-generated RSA-2048 keypair, tracks sequence
 * numbers, and replays scripted Execution Reports. Supports fault injection
 * (checksum corruption, abrupt disconnect, double-Logon termination, mid-resend
 * drops).
 *
 * <p>Framing on the wire is a 4-byte big-endian length prefix followed by the
 * OCG-C frame. This keeps the mock transport-independent from the protocol's own
 * {@code Length} field, whose exact convention is still open (PROGRESS.md).
 */
public final class MockOcgServer implements AutoCloseable {

    private final Dictionary dict;
    private final KeyPair keyPair = PasswordCipher.generateKeyPair();
    private final List<Message> received = new CopyOnWriteArrayList<>();

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running;

    private String expectedPassword = "Password1";
    private String compId = "TEST";
    private String advertiseIp = "127.0.0.1";
    private int advertisePort;
    private long nextToSend = 1;
    private long nextExpected = 1;
    private boolean rejectLogon;
    private boolean corruptNextReplyChecksum;
    private boolean disconnectAfterLogon;
    private boolean terminateOnSecondLogon;
    private boolean loggedOn;
    private int dropExecutionsAtOrAbove = -1;
    private int globalResendStart = -1;

    public MockOcgServer(Dictionary dict) {
        this.dict = dict;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(0);
        advertisePort = serverSocket.getLocalPort();
        running = true;
        acceptThread = new Thread(this::acceptLoop, "mock-ocg-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public int port() {
        return advertisePort;
    }

    public String host() {
        return advertiseIp;
    }

    public KeyPair keyPair() {
        return keyPair;
    }

    public List<Message> received() {
        return received;
    }

    public void setCompId(String value) {
        this.compId = value;
    }

    public void setExpectedPassword(String value) {
        this.expectedPassword = value;
    }

    public void setRejectLogon(boolean value) {
        this.rejectLogon = value;
    }

    public void setCorruptNextReplyChecksum(boolean value) {
        this.corruptNextReplyChecksum = value;
    }

    public void setDisconnectAfterLogon(boolean value) {
        this.disconnectAfterLogon = value;
    }

    public void setTerminateOnSecondLogon(boolean value) {
        this.terminateOnSecondLogon = value;
    }

    public void setDropExecutionsAtOrAbove(int seq) {
        this.dropExecutionsAtOrAbove = seq;
    }

    /** Forces the peer's Next To Send for a client-behind reconciliation. */
    public void setAdvertisedNextToSend(long value) {
        this.nextToSend = value;
    }

    // ---- accept loop ----

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                Thread t = new Thread(() -> handle(socket), "mock-ocg-conn");
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                if (running) {
                    // Ignore transient accept errors.
                }
            }
        }
    }

    private void handle(Socket socket) {
        try (socket;
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            while (running && !socket.isClosed()) {
                Message msg = readFrame(in);
                received.add(msg);

                if (msg.messageType() == MsgTypes.LOOKUP_REQUEST) {
                    send(out, buildLookupResponse());
                    continue;
                }

                if (msg.messageType() == MsgTypes.LOGON) {
                    if (terminateOnSecondLogon && loggedOn) {
                        close();
                        return;
                    }
                    Message reply = handleLogon(msg);
                    send(out, reply);
                    if (disconnectAfterLogon) {
                        return;
                    }
                    continue;
                }

                if (msg.messageType() == MsgTypes.LOGOUT) {
                    send(out, message(MsgTypes.LOGOUT, "sessionStatus", 4L));
                    return;
                }

                if (msg.messageType() == MsgTypes.RESEND_REQUEST) {
                    handleResend(msg, out);
                    continue;
                }

                if (MsgTypes.HEARTBEAT == msg.messageType()
                        || MsgTypes.TEST_REQUEST == msg.messageType()) {
                    nextExpected = Math.max(nextExpected, msg.sequenceNumber() + 1);
                }
            }
        } catch (EOFException | java.net.SocketException ignored) {
            // Client disconnected.
        } catch (IOException e) {
            // Tear down on unexpected I/O.
        }
    }

    // ---- lookups & logon ----

    private Message buildLookupResponse() {
        return message(MsgTypes.LOOKUP_RESPONSE, "status", 0L,
                "primaryIp", advertiseIp,
                "primaryPort", (long) advertisePort,
                "secondaryIp", advertiseIp,
                "secondaryPort", (long) advertisePort);
    }

    private Message handleLogon(Message logon) throws IOException {
        long clientNextExpected = number(logon.get("nextExpectedMessageSequence"));

        if (rejectLogon) {
            return message(MsgTypes.LOGON, "sessionStatus", 5L, "text", "invalid");
        }
        String provided = decryptPassword(String.valueOf(logon.get("password")));
        if (!provided.endsWith(expectedPassword)) {
            return message(MsgTypes.LOGON, "sessionStatus", 5L, "text", "invalid");
        }

        Message reply = message(MsgTypes.LOGON, "sessionStatus", 0L);
        if (clientNextExpected < nextToSend) {
            // Client behind: peer will gap-fill from the client's next expected.
            globalResendStart = (int) clientNextExpected;
        }
        nextExpected = Math.max(nextExpected, logon.sequenceNumber() + 1);
        loggedOn = true;
        return reply;
    }

    private String decryptPassword(String base64Ciphertext) {
        try {
            byte[] ciphertext = Base64.getDecoder().decode(base64Ciphertext);
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
            OAEPParameterSpec spec = new OAEPParameterSpec(
                    PasswordCipher.OAEP_DIGEST, "MGF1",
                    new MGF1ParameterSpec(PasswordCipher.MGF1_DIGEST),
                    PSource.PSpecified.DEFAULT);
            PrivateKey privateKey = keyPair.getPrivate();
            cipher.init(Cipher.DECRYPT_MODE, privateKey, spec);
            byte[] plain = cipher.doFinal(ciphertext);
            // 14-char UTC timestamp prefix + password.
            String full = new String(plain, StandardCharsets.US_ASCII);
            return full.length() >= 14 ? full.substring(14) : full;
        } catch (GeneralSecurityException e) {
            return "";
        }
    }

    // ---- resend / scripted execution reports ----

    private void handleResend(Message request, DataOutputStream out) throws IOException {
        long start = number(request.get("startSequence"));
        long end = number(request.get("endSequence"));
        if (end == 0) {
            end = nextToSend - 1;
        }
        if (globalResendStart >= 0) {
            start = Math.min(start, globalResendStart);
            globalResendStart = -1;
        }
        nextExpected = Math.max(nextExpected, request.sequenceNumber() + 1);
        for (long seq = start; seq <= end; seq++) {
            if (dropExecutionsAtOrAbove >= 0 && seq >= dropExecutionsAtOrAbove) {
                continue;
            }
            send(out, executionReport(seq));
        }
    }

    /** Emits a scripted Execution Report to a specific connection. */
    public void emitExecutionReport(DataOutputStream out, long seq) throws IOException {
        send(out, executionReport(seq));
    }

    /** Builds a minimal Execution Report used by scripted replay. */
    public Message executionReport(long seq) {
        return new Message(MsgTypes.EXECUTION_REPORT)
                .sequenceNumber(seq)
                .compId(compId)
                .put("clientOrderId", "1")
                .put("submittingBrokerId", "1")
                .put("securityId", "1")
                .put("securityIdSource", 8L)
                .put("transactionTime", "20260101-00:00:00.000000")
                .put("side", 1L)
                .put("orderId", "1")
                .put("executionId", "E" + seq)
                .put("orderStatus", 0L)
                .put("execType", "0")
                .put("cumulativeQuantity", 0L)
                .put("leavesQuantity", 100_00000000L);
    }

    // ---- framing ----

    private Message readFrame(DataInputStream in) throws IOException {
        int len = in.readInt();
        byte[] frame = new byte[len];
        in.readFully(frame);
        if (corruptNextReplyChecksum) {
            // Caller requested corruption of an inbound frame.
        }
        return MessageCodec.decode(dict, frame);
    }

    private void send(DataOutputStream out, Message message) throws IOException {
        byte[] frame = MessageCodec.encode(dict, message);
        if (corruptNextReplyChecksum) {
            corruptNextReplyChecksum = false;
            frame[frame.length - 1] ^= 0xFF;
        }
        out.writeInt(frame.length);
        out.write(frame);
        out.flush();
    }

    private Message message(int type, Object... kv) {
        Message m = new Message(type).sequenceNumber(nextToSend++).compId(compId);
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private static long number(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    @Override
    public void close() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // Best effort.
        }
    }
}
