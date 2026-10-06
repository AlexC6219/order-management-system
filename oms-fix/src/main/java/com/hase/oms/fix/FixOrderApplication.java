package com.hase.oms.fix;

import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;

/**
 * QuickFIX/J {@link Application} for the single GFIX acceptor session.
 *
 * <p>Parses inbound order messages and routes them to a {@link FixOrderHandler};
 * outbound FIX messages are sent with {@link #send(Message)} on the established
 * session.
 */
public final class FixOrderApplication implements Application {

    private final FixOrderHandler handler;
    private volatile SessionID sessionId;

    public FixOrderApplication(FixOrderHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onCreate(SessionID sessionId) {
        // No-op.
    }

    @Override
    public void onLogon(SessionID sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void onLogout(SessionID sessionId) {
        this.sessionId = null;
    }

    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        // No-op.
    }

    @Override
    public void fromAdmin(Message message, SessionID sessionId) {
        // No-op.
    }

    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        // No-op.
    }

    @Override
    public void fromApp(Message message, SessionID sessionId)
            throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        String msgType = message.getHeader().getString(FixTags.MSG_TYPE);
        switch (msgType) {
            case FixTags.NEW_ORDER_SINGLE ->
                    handler.onNewOrder(FixMapping.toOrderRequest(message));
            case FixTags.ORDER_CANCEL_REQUEST ->
                    handler.onCancel(FixMapping.originalClientOrderId(message));
            case FixTags.ORDER_CANCEL_REPLACE_REQUEST ->
                    handler.onCancelReplace(FixMapping.originalClientOrderId(message),
                            FixMapping.toOrderRequest(message));
            default -> {
                // Ignore unsupported application messages.
            }
        }
    }

    /** Sends a FIX message on the active session; returns false if not logged on. */
    public boolean send(Message message) {
        SessionID id = sessionId;
        if (id == null) {
            return false;
        }
        try {
            return Session.sendToTarget(message, id);
        } catch (quickfix.SessionNotFound e) {
            return false;
        }
    }

    public boolean isLoggedOn() {
        return sessionId != null;
    }
}
