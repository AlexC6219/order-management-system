package com.hase.oms.fix;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FileStoreFactory;
import quickfix.ScreenLogFactory;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;

/** Starts/stops the QuickFIX/J acceptor for the GFIX session. */
public final class FixAcceptor implements AutoCloseable {

    private SocketAcceptor acceptor;

    public void start(SessionSettings settings, Application application) throws ConfigError {
        acceptor = new SocketAcceptor(
                application,
                new FileStoreFactory(settings),
                settings,
                new ScreenLogFactory(false, false, false),
                new DefaultMessageFactory());
        acceptor.start();
    }

    public void stop() {
        if (acceptor != null) {
            acceptor.stop();
        }
    }

    @Override
    public void close() {
        stop();
    }
}
