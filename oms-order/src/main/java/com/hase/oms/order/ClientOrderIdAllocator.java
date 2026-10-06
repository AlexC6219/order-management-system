package com.hase.oms.order;

/**
 * Allocates OCG-C {@code Client Order ID}s: numeric 1..99,999,999, no leading
 * zeros, unique per Submitting Broker ID per day (DESIGN.md §9.2, FR-4).
 *
 * <p>Every New/Amend/Cancel consumes a fresh ID; the caller links it to the
 * prior order via {@link Order#originalClientOrderId(Long)}.
 */
public final class ClientOrderIdAllocator {

    public static final long MIN = 1L;
    public static final long MAX = 99_999_999L;

    private long next = MIN;

    public synchronized long allocate() {
        if (next > MAX) {
            throw new IllegalStateException("Client Order ID chain exhausted at " + MAX);
        }
        return next++;
    }

    public synchronized long nextAvailable() {
        return next;
    }

    /** Resets the chain at the start of a new trading day. */
    public synchronized void resetDaily() {
        next = MIN;
    }
}
