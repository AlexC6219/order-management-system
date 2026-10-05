package com.hase.oms.session;

import java.time.Duration;

/**
 * Timing and identity parameters for an OCG-C session.
 *
 * <p>Defaults mirror DESIGN.md §7 / TESTING.md §1.1: 20s heartbeat, Test Request
 * after 3 missed intervals, Logout after ~3 more, 5s lookup retry, 60s logon and
 * logout timeouts, 10s reconnect delay.
 */
public final class SessionConfig {

    private String compId = "";
    private Duration heartbeatInterval = Duration.ofSeconds(20);
    private int missedIntervalsBeforeTestRequest = 3;
    private int missedIntervalsBeforeLogout = 3;
    private Duration lookupRetryDelay = Duration.ofSeconds(5);
    private Duration logonTimeout = Duration.ofSeconds(60);
    private Duration logoutTimeout = Duration.ofSeconds(60);
    private Duration reconnectDelay = Duration.ofSeconds(10);

    public String compId() {
        return compId;
    }

    public SessionConfig compId(String value) {
        this.compId = value;
        return this;
    }

    public Duration heartbeatInterval() {
        return heartbeatInterval;
    }

    public SessionConfig heartbeatInterval(Duration value) {
        this.heartbeatInterval = value;
        return this;
    }

    public int missedIntervalsBeforeTestRequest() {
        return missedIntervalsBeforeTestRequest;
    }

    public SessionConfig missedIntervalsBeforeTestRequest(int value) {
        this.missedIntervalsBeforeTestRequest = value;
        return this;
    }

    public int missedIntervalsBeforeLogout() {
        return missedIntervalsBeforeLogout;
    }

    public SessionConfig missedIntervalsBeforeLogout(int value) {
        this.missedIntervalsBeforeLogout = value;
        return this;
    }

    public Duration lookupRetryDelay() {
        return lookupRetryDelay;
    }

    public SessionConfig lookupRetryDelay(Duration value) {
        this.lookupRetryDelay = value;
        return this;
    }

    public Duration logonTimeout() {
        return logonTimeout;
    }

    public SessionConfig logonTimeout(Duration value) {
        this.logonTimeout = value;
        return this;
    }

    public Duration logoutTimeout() {
        return logoutTimeout;
    }

    public SessionConfig logoutTimeout(Duration value) {
        this.logoutTimeout = value;
        return this;
    }

    public Duration reconnectDelay() {
        return reconnectDelay;
    }

    public SessionConfig reconnectDelay(Duration value) {
        this.reconnectDelay = value;
        return this;
    }

    /** Absolute time at which a Test Request should be sent (idle). */
    public Duration testRequestAfter() {
        return heartbeatInterval.multipliedBy(missedIntervalsBeforeTestRequest);
    }

    /** Absolute time at which the session should be logged out (idle). */
    public Duration logoutAfter() {
        return heartbeatInterval.multipliedBy(
                (long) missedIntervalsBeforeTestRequest + missedIntervalsBeforeLogout);
    }
}
