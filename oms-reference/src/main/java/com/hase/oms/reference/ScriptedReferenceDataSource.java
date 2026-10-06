package com.hase.oms.reference;

import java.util.List;
import java.util.function.Consumer;

/**
 * Deterministic in-memory {@link ReferenceDataSource} for tests and demos. On
 * {@link #publishAll()} it emits every configured update to the subscriber.
 */
public final class ScriptedReferenceDataSource implements ReferenceDataSource {

    private final List<ReferenceUpdate> updates;
    private Consumer<ReferenceUpdate> listener;
    private boolean started;

    public ScriptedReferenceDataSource(List<ReferenceUpdate> updates) {
        this.updates = List.copyOf(updates);
    }

    @Override
    public void subscribe(Consumer<ReferenceUpdate> listener) {
        this.listener = listener;
    }

    @Override
    public void start() {
        started = true;
    }

    @Override
    public void stop() {
        started = false;
    }

    /** Emits all configured updates in order. */
    public void publishAll() {
        if (!started) {
            throw new IllegalStateException("Source not started");
        }
        if (listener != null) {
            updates.forEach(listener);
        }
    }
}
