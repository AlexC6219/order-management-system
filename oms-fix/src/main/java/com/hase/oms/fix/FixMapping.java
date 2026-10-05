package com.hase.oms.fix;

import com.hase.oms.codec.enums.OrderType;
import com.hase.oms.codec.enums.Side;
import com.hase.oms.codec.enums.Tif;
import com.hase.oms.order.Order;
import com.hase.oms.order.OrderEvent;
import com.hase.oms.order.OrderRequest;
import com.hase.oms.order.OrderState;
import quickfix.FieldNotFound;
import quickfix.Message;

/**
 * FIX 5.0 SP2 ↔ internal order mapping (DESIGN.md §8).
 *
 * <p>Uses the generic {@link quickfix.Message} with tag numbers so the mapping is
 * testable without a live session. The acceptor ({@link FixOrderApplication})
 * is the transport that carries these messages.
 */
public final class FixMapping {

    public static final String BEGIN_STRING_VALUE = "FIXT.1.1";
    private static final double SCALE = 1e8;

    private FixMapping() {}

    // ---- FIX → internal ----

    public static OrderRequest toOrderRequest(Message nos) throws FieldNotFound {
        String clOrdId = nos.getString(FixTags.CL_ORD_ID);
        String account = nos.isSetField(FixTags.ACCOUNT) ? nos.getString(FixTags.ACCOUNT) : "";
        String symbol = nos.getString(FixTags.SYMBOL);
        Side side = sideFromFix(nos.getChar(FixTags.SIDE));
        OrderType orderType = orderTypeFromFix(nos.getChar(FixTags.ORD_TYPE));
        Tif tif = tifFromFix(nos.isSetField(FixTags.TIME_IN_FORCE)
                ? nos.getChar(FixTags.TIME_IN_FORCE) : '0');
        long price = nos.isSetField(FixTags.PRICE) ? scale(nos.getDouble(FixTags.PRICE)) : 0L;
        long quantity = scale(nos.getDouble(FixTags.ORDER_QTY));
        String text = nos.isSetField(FixTags.TEXT) ? nos.getString(FixTags.TEXT) : null;
        return new OrderRequest(clOrdId, account, symbol, side, orderType, tif, price, quantity, text);
    }

    public static long originalClientOrderId(Message m) throws FieldNotFound {
        return Long.parseLong(m.getString(FixTags.ORIG_CL_ORD_ID));
    }

    // ---- internal → FIX ----

    /** Builds a FIX {@code ExecutionReport (8)} for a non-reject order event. */
    public static Message executionReport(Order order, OrderEvent event) {
        Message m = new Message();
        m.getHeader().setString(FixTags.BEGIN_STRING, BEGIN_STRING_VALUE);
        m.getHeader().setString(FixTags.MSG_TYPE, FixTags.EXECUTION_REPORT);

        m.setString(FixTags.CL_ORD_ID, Long.toString(order.clientOrderId()));
        if (order.originalClientOrderId() != null) {
            m.setString(FixTags.ORIG_CL_ORD_ID, Long.toString(order.originalClientOrderId()));
        }
        if (order.exchangeOrderId() != null) {
            m.setString(FixTags.ORDER_ID, order.exchangeOrderId());
        }
        m.setString(FixTags.SYMBOL, order.securityId());
        m.setChar(FixTags.SIDE, fixSide(order.side()));
        m.setChar(FixTags.ORD_TYPE, fixOrderType(order.orderType()));
        m.setChar(FixTags.TIME_IN_FORCE, fixTif(order.tif()));
        m.setDouble(FixTags.ORDER_QTY, unscale(order.quantity()), 8);
        m.setDouble(FixTags.CUM_QTY, unscale(order.cumulativeQuantity()), 8);
        m.setDouble(FixTags.LEAVES_QTY, unscale(order.leavesQuantity()), 8);

        m.setChar(FixTags.EXEC_TYPE, fixExecType(order, event));
        m.setChar(FixTags.ORD_STATUS, fixOrdStatus(order, event));
        m.setString(FixTags.EXEC_ID, execId(order, event));
        return m;
    }

    /**
     * Builds a FIX {@code OrderCancelReject (9)} synthesized from OCG-C Exec Type
     * {@code 'X'} (cancel reject) or {@code 'Y'} (amend reject) — DESIGN.md §8.2.
     */
    public static Message orderCancelReject(Order order, OrderEvent event) {
        Message m = new Message();
        m.getHeader().setString(FixTags.BEGIN_STRING, BEGIN_STRING_VALUE);
        m.getHeader().setString(FixTags.MSG_TYPE, FixTags.ORDER_CANCEL_REJECT);

        m.setString(FixTags.CL_ORD_ID, Long.toString(order.clientOrderId()));
        if (order.originalClientOrderId() != null) {
            m.setString(FixTags.ORIG_CL_ORD_ID, Long.toString(order.originalClientOrderId()));
        }
        if (order.exchangeOrderId() != null) {
            m.setString(FixTags.ORDER_ID, order.exchangeOrderId());
        }
        m.setChar(FixTags.ORD_STATUS, fixOrdStatus(order, event));

        if (event instanceof OrderEvent.AmendReject ar) {
            m.setChar(FixTags.CXL_REJ_RESPONSE_TO, '2'); // response to Order Cancel/Replace
            m.setInt(FixTags.CXL_REJ_REASON, (int) ar.rejectCode());
        } else if (event instanceof OrderEvent.CancelReject cr) {
            m.setChar(FixTags.CXL_REJ_RESPONSE_TO, '1'); // response to Order Cancel
            m.setInt(FixTags.CXL_REJ_REASON, (int) cr.rejectCode());
        }
        return m;
    }

    // ---- helpers ----

    private static char fixExecType(Order order, OrderEvent event) {
        if (event instanceof OrderEvent.NewAck) return '0';
        if (event instanceof OrderEvent.Reject) return '8';
        if (event instanceof OrderEvent.Expire) return 'C';
        if (event instanceof OrderEvent.Fill) return 'F';
        if (event instanceof OrderEvent.Cancel) return '4';
        if (event instanceof OrderEvent.Amend) return '5';
        if (event instanceof OrderEvent.TradeCancel) return 'H';
        if (event instanceof OrderEvent.CancelReject) return 'X';
        if (event instanceof OrderEvent.AmendReject) return 'Y';
        return '0';
    }

    private static char fixOrdStatus(Order order, OrderEvent event) {
        if (event instanceof OrderEvent.NewAck) return '0';
        if (event instanceof OrderEvent.Reject) return '8';
        if (event instanceof OrderEvent.Expire) return 'C';
        if (event instanceof OrderEvent.Cancel) return '4';
        if (event instanceof OrderEvent.Fill) {
            return order.leavesQuantity() == 0 ? '2' : '1';
        }
        if (event instanceof OrderEvent.Amend) {
            return ordStatusForLiveState(order.state());
        }
        if (event instanceof OrderEvent.TradeCancel) {
            return ordStatusForLiveState(order.state());
        }
        if (event instanceof OrderEvent.CancelReject || event instanceof OrderEvent.AmendReject) {
            return ordStatusForLiveState(order.state());
        }
        return ordStatusForLiveState(order.state());
    }

    private static char ordStatusForLiveState(OrderState state) {
        return switch (state) {
            case PARTIALLY_FILLED -> '1';
            case FILLED -> '2';
            case CANCELLED -> '4';
            case REJECTED -> '8';
            case EXPIRED -> 'C';
            default -> '0';
        };
    }

    private static String execId(Order order, OrderEvent event) {
        if (event instanceof OrderEvent.NewAck) {
            return order.exchangeOrderId() != null ? order.exchangeOrderId() : "0";
        }
        if (event instanceof OrderEvent.Fill f) return f.executionId();
        if (event instanceof OrderEvent.Cancel c) return c.executionId();
        if (event instanceof OrderEvent.Amend a) return a.executionId();
        if (event instanceof OrderEvent.Expire e) return e.executionId();
        if (event instanceof OrderEvent.TradeCancel t) return t.executionId();
        if (event instanceof OrderEvent.CancelReject cr) return cr.executionId();
        if (event instanceof OrderEvent.AmendReject ar) return ar.executionId();
        return "0";
    }

    private static Side sideFromFix(char c) {
        return switch (c) {
            case '1' -> Side.Buy;
            case '2' -> Side.Sell;
            case '5' -> Side.SellShort;
            default -> throw new IllegalArgumentException("Unknown FIX Side: " + c);
        };
    }

    private static OrderType orderTypeFromFix(char c) {
        return switch (c) {
            case '1' -> OrderType.Market;
            case '2' -> OrderType.Limit;
            default -> throw new IllegalArgumentException("Unknown FIX OrdType: " + c);
        };
    }

    private static Tif tifFromFix(char c) {
        return switch (c) {
            case '0' -> Tif.Day;
            case '3' -> Tif.IOC;
            case '4' -> Tif.FOK;
            case '9' -> Tif.AtCrossing;
            default -> throw new IllegalArgumentException("Unknown FIX TimeInForce: " + c);
        };
    }

    private static char fixSide(Side side) {
        return switch (side) {
            case Buy -> '1';
            case Sell -> '2';
            case SellShort -> '5';
        };
    }

    private static char fixOrderType(OrderType type) {
        return type == OrderType.Market ? '1' : '2';
    }

    private static char fixTif(Tif tif) {
        return switch (tif) {
            case Day -> '0';
            case IOC -> '3';
            case FOK -> '4';
            case AtCrossing -> '9';
        };
    }

    private static long scale(double value) {
        return Math.round(value * SCALE);
    }

    private static double unscale(long scaled) {
        return (double) scaled / SCALE;
    }
}
