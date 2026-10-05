package com.hase.oms.session;

/**
 * Tracks outbound and inbound sequence numbers for one OCG-C session.
 *
 * <p>Both counters initialise to 1 each trading day (DESIGN.md §7.1). The
 * inbound counter ("next expected") and outbound counter ("next to send") are
 * plain 32-bit unsigned values held as {@code long}.
 */
public final class SequenceTracker {

    private long nextToSend;
    private long nextExpected;

    public SequenceTracker() {
        this(1L, 1L);
    }

    public SequenceTracker(long nextToSend, long nextExpected) {
        this.nextToSend = nextToSend;
        this.nextExpected = nextExpected;
    }

    public long nextToSend() {
        return nextToSend;
    }

    public long nextExpected() {
        return nextExpected;
    }

    /** Allocates the next outbound sequence number and advances the counter. */
    public long allocateOutbound() {
        return nextToSend++;
    }

    /**
     * Records an inbound message without gap checking. Used for test/reset
     * messages that are not sequence-bearing.
     */
    public void acceptInbound() {
        nextExpected++;
    }

    public void setNextToSend(long value) {
        this.nextToSend = value;
    }

    public void setNextExpected(long value) {
        this.nextExpected = value;
    }
}
