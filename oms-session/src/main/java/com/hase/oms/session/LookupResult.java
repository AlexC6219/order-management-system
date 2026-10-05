package com.hase.oms.session;

import com.hase.oms.codec.Message;

/**
 * Result of a Lookup Service exchange: the endpoint to connect to, or a reject.
 */
public final class LookupResult {

    private final boolean accepted;
    private final String primaryIp;
    private final int primaryPort;
    private final String secondaryIp;
    private final int secondaryPort;
    private final long rejectCode;
    private final String reason;

    private LookupResult(boolean accepted, String primaryIp, int primaryPort,
                         String secondaryIp, int secondaryPort,
                         long rejectCode, String reason) {
        this.accepted = accepted;
        this.primaryIp = primaryIp;
        this.primaryPort = primaryPort;
        this.secondaryIp = secondaryIp;
        this.secondaryPort = secondaryPort;
        this.rejectCode = rejectCode;
        this.reason = reason;
    }

    public static LookupResult accepted(String primaryIp, int primaryPort,
                                        String secondaryIp, int secondaryPort) {
        return new LookupResult(true, primaryIp, primaryPort, secondaryIp, secondaryPort, 0, null);
    }

    public static LookupResult rejected(long rejectCode, String reason) {
        return new LookupResult(false, null, 0, null, 0, rejectCode, reason);
    }

    /** Parses a decoded Lookup Response. */
    public static LookupResult fromResponse(Message response) {
        long status = number(response.get("status"));
        if (status != 0L) {
            return rejected(number(response.get("lookupRejectCode")), string(response.get("reason")));
        }
        return accepted(
                string(response.get("primaryIp")),
                (int) number(response.get("primaryPort")),
                string(response.get("secondaryIp")),
                (int) number(response.get("secondaryPort")));
    }

    private static long number(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static String string(Object value) {
        return value == null ? "" : (String) value;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String primaryIp() {
        return primaryIp;
    }

    public int primaryPort() {
        return primaryPort;
    }

    public String secondaryIp() {
        return secondaryIp;
    }

    public int secondaryPort() {
        return secondaryPort;
    }

    public long rejectCode() {
        return rejectCode;
    }

    public String reason() {
        return reason;
    }
}
