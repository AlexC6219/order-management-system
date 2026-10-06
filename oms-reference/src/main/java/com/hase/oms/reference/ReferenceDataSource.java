package com.hase.oms.reference;

import java.util.function.Consumer;

/**
 * Pluggable reference-data source (OMD-C in production, Refinitiv optionally
 * later, file/scripted in tests). Publishes {@link ReferenceUpdate}s to a
 * subscriber, which feeds the {@link PhaseAwareReferenceCache}.
 */
public interface ReferenceDataSource extends AutoCloseable {

    void subscribe(Consumer<ReferenceUpdate> listener);

    void start();

    void stop();

    @Override
    default void close() {
        stop();
    }
}
