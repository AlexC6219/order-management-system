package com.hase.oms.fix;

import com.hase.oms.codec.Message;
import com.hase.oms.order.ClientOrderIdAllocator;
import com.hase.oms.order.Order;
import com.hase.oms.order.OrderEvent;
import com.hase.oms.order.OrderManager;
import com.hase.oms.order.OrderRequest;
import com.hase.oms.order.OrderTranslator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quickfix.Application;
import quickfix.DefaultMessageFactory;
import quickfix.FileStoreFactory;
import quickfix.MessageStoreFactory;
import quickfix.ScreenLogFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;

import java.io.ByteArrayInputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * L3 integration: Mock GFIX (QuickFIX/J initiator) → OMS FIX acceptor →
 * OrderManager → OCG-C messages, then an Execution Report back to FIX.
 */
class FixOrderFlowL3Test {

    private final List<Message> ocgOutbound = new ArrayList<>();
    private final GfixApplication gfixApp = new GfixApplication();
    private final FixAcceptor acceptor = new FixAcceptor();
    private FixOrderApplication acceptorApp;
    private SocketInitiator initiator;
    private OrderManager orderManager;
    private Path accStore;
    private Path initStore;

    @BeforeEach
    void setUp() throws Exception {
        OrderTranslator translator = new OrderTranslator("OMS", Clock.systemUTC());
        orderManager = new OrderManager(new ClientOrderIdAllocator(), translator, ocgOutbound::add);

        acceptorApp = new FixOrderApplication(new FixOrderHandler() {
            @Override
            public void onNewOrder(OrderRequest request) {
                orderManager.submit(request);
            }

            @Override
            public void onCancel(long originalClientOrderId) {
                orderManager.repository().byClientOrderId(originalClientOrderId)
                        .ifPresent(orderManager::cancel);
            }

            @Override
            public void onCancelReplace(long originalClientOrderId, OrderRequest replacement) {
                orderManager.repository().byClientOrderId(originalClientOrderId)
                        .ifPresent(order -> orderManager.amend(order, replacement.price(), replacement.quantity()));
            }
        });

        int port = freePort();
        accStore = Files.createTempDirectory("fix-acc");
        initStore = Files.createTempDirectory("fix-init");

        acceptor.start(settings("acceptor", port, "OMS", "GFIX", accStore), acceptorApp);

        SessionSettings initSettings = settings("initiator", port, "GFIX", "OMS", initStore);
        MessageStoreFactory storeFactory = new FileStoreFactory(initSettings);
        initiator = new SocketInitiator(gfixApp, storeFactory, initSettings,
                new ScreenLogFactory(false, false, false), new DefaultMessageFactory());
        initiator.start();

        await(() -> acceptorApp.isLoggedOn() && gfixApp.isLoggedOn(), "FIX session did not log on");
    }

    @AfterEach
    void tearDown() {
        if (initiator != null) {
            initiator.stop();
        }
        acceptor.stop();
    }

    @Test
    void newOrderFlowsToOcgAndExecutionReportReturnsToFix() throws Exception {
        gfixApp.send(newOrderSingle("UP-1"));

        await(() -> !ocgOutbound.isEmpty(), "no OCG-C New Order emitted");
        Message ocg = ocgOutbound.get(0);
        assertEquals(11, ocg.messageType());
        assertEquals("1", ocg.get("clientOrderId"));
        assertEquals("BROKER1", ocg.get("submittingBrokerId"));
        assertEquals("5", ocg.get("securityId"));

        Order order = orderManager.repository().byClientOrderId(1L).orElseThrow();
        assertTrue(orderManager.onExecutionReport(new Message(10)
                .put("clientOrderId", "1").put("execType", "0").put("executionId", "E0")
                .put("orderId", "O1")));

        assertTrue(acceptorApp.send(FixMapping.executionReport(order, new OrderEvent.NewAck("O1"))));
        quickfix.Message fixEr = gfixApp.awaitExecutionReport();
        assertNotNull(fixEr);
        assertEquals("8", fixEr.getHeader().getString(FixTags.MSG_TYPE));
        assertEquals("1", fixEr.getString(FixTags.CL_ORD_ID));
        assertEquals('0', fixEr.getChar(FixTags.EXEC_TYPE));
    }

    // ---- helpers ----

    private static quickfix.Message newOrderSingle(String clOrdId) {
        quickfix.Message m = new quickfix.Message();
        m.getHeader().setString(FixTags.MSG_TYPE, FixTags.NEW_ORDER_SINGLE);
        m.setString(FixTags.CL_ORD_ID, clOrdId);
        m.setString(FixTags.SYMBOL, "5");
        m.setChar(FixTags.SIDE, '1');
        m.setChar(FixTags.ORD_TYPE, '2');
        m.setDouble(FixTags.ORDER_QTY, 1000.0, 8);
        m.setDouble(FixTags.PRICE, 78.5, 8);
        m.setChar(FixTags.TIME_IN_FORCE, '0');
        m.setString(FixTags.TRANSACT_TIME, "20260304-05:06:07.000000");
        m.setString(FixTags.ACCOUNT, "BROKER1");
        return m;
    }

    private static SessionSettings settings(String type, int port, String sender, String target, Path store)
            throws Exception {
        String cfg = String.join("\n",
                "[SESSION]",
                "BeginString=FIXT.1.1",
                "SenderCompID=" + sender,
                "TargetCompID=" + target,
                "DefaultApplVerID=FIX.5.0SP2",
                "TransportDataDictionary=FIXT11.xml",
                "AppDataDictionary=FIX50SP2.xml",
                "ConnectionType=" + type,
                "StartTime=00:00:00",
                "EndTime=23:59:59",
                "HeartBtInt=5",
                "ResetOnLogon=Y",
                "FileStorePath=" + store.toAbsolutePath(),
                "FileLogPath=" + store.resolve("log").toAbsolutePath(),
                type.equals("acceptor") ? "SocketAcceptPort=" + port : "SocketConnectHost=127.0.0.1",
                type.equals("acceptor") ? "" : "SocketConnectPort=" + port);
        return new SessionSettings(new ByteArrayInputStream(cfg.getBytes(StandardCharsets.UTF_8)));
    }

    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void await(BooleanSupplier condition, String message) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(25);
        }
        fail(message);
    }

    /** Mock GFIX: captures Execution Reports and tracks logon. */
    private static final class GfixApplication implements Application {
        private final BlockingQueue<quickfix.Message> executionReports = new LinkedBlockingQueue<>();
        private volatile boolean loggedOn;
        private volatile SessionID sessionId;

        @Override
        public void onCreate(SessionID sessionId) {
        }

        @Override
        public void onLogon(SessionID sessionId) {
            this.sessionId = sessionId;
            loggedOn = true;
        }

        @Override
        public void onLogout(SessionID sessionId) {
            loggedOn = false;
        }

        @Override
        public void toAdmin(quickfix.Message message, SessionID sessionId) {
        }

        @Override
        public void fromAdmin(quickfix.Message message, SessionID sessionId) {
        }

        @Override
        public void toApp(quickfix.Message message, SessionID sessionId) {
        }

        @Override
        public void fromApp(quickfix.Message message, SessionID sessionId) throws quickfix.FieldNotFound {
            if (FixTags.EXECUTION_REPORT.equals(message.getHeader().getString(FixTags.MSG_TYPE))) {
                executionReports.add(message);
            }
        }

        boolean isLoggedOn() {
            return loggedOn;
        }

        void send(quickfix.Message message) {
            try {
                Session.sendToTarget(message, sessionId);
            } catch (quickfix.SessionNotFound e) {
                throw new IllegalStateException(e);
            }
        }

        quickfix.Message awaitExecutionReport() throws InterruptedException {
            return executionReports.poll(5, TimeUnit.SECONDS);
        }
    }
}
