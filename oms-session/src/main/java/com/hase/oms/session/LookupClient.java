package com.hase.oms.session;

import com.hase.oms.codec.Dictionary;
import com.hase.oms.codec.Message;
import com.hase.oms.codec.MessageCodec;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Lookup Service client.
 *
 * <p>Walks the configured preference order (primary, mirror, backup-primary,
 * backup-mirror — represented here as an ordered list of endpoints), sending a
 * Lookup Request(7) with sequence number 1 (DESIGN.md §7.1). On failure it waits
 * {@link SessionConfig#lookupRetryDelay()} and advances to the next endpoint,
 * cycling.
 */
public final class LookupClient {

    private final Dictionary dict;
    private final SessionMessages messages;
    private final Endpoint[] endpoints;
    private final int typeOfService;
    private final int protocolType;

    private int nextIndex;

    public LookupClient(Dictionary dict, SessionConfig config, java.util.List<Endpoint> endpoints,
                        int typeOfService, int protocolType) {
        this.dict = dict;
        this.messages = new SessionMessages(dict, config.compId());
        this.endpoints = endpoints.toArray(new Endpoint[0]);
        if (this.endpoints.length == 0) {
            throw new IllegalArgumentException("At least one lookup endpoint is required");
        }
        this.typeOfService = typeOfService;
        this.protocolType = protocolType;
    }

    /**
     * Performs one lookup attempt against the current endpoint. Returns the
     * decoded response message, or {@code null} if the endpoint was unreachable.
     */
    public Message attempt() {
        Endpoint endpoint = endpoints[nextIndex];
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.host(), endpoint.port()),
                    (int) java.time.Duration.ofSeconds(5).toMillis());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            byte[] request = MessageCodec.encode(dict, messages.lookupRequest(1L, typeOfService, protocolType));
            out.writeInt(request.length);
            out.write(request);
            out.flush();

            int len = in.readInt();
            byte[] frame = new byte[len];
            in.readFully(frame);
            return MessageCodec.decode(dict, frame);
        } catch (IOException e) {
            return null;
        }
    }

    /** Returns the endpoint the next {@link #attempt()} will use. */
    public Endpoint currentEndpoint() {
        return endpoints[nextIndex];
    }

    /** Advances to the next endpoint in the preference order, cycling. */
    public Endpoint advance() {
        nextIndex = (nextIndex + 1) % endpoints.length;
        return endpoints[nextIndex];
    }

    /** Endpoint used for Lookup and for the returned session connection. */
    public record Endpoint(String host, int port) {}
}
