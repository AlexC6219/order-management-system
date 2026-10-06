package com.hase.oms.session;

import com.hase.oms.codec.Dictionary;
import com.hase.oms.codec.Message;
import com.hase.oms.harness.mock.MockOcgServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** L2 integration tests: Lookup Service client against the Mock OCG-C server. */
class LookupClientTest {

    private Dictionary dict;
    private MockOcgServer server;

    @BeforeEach
    void setUp() throws IOException {
        dict = SessionEngineTest.dict();
        server = new MockOcgServer(dict);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void lookupReturnsAdvertisedEndpoint() {
        SessionConfig config = new SessionConfig().compId("TEST");
        LookupClient client = new LookupClient(dict, config,
                List.of(new LookupClient.Endpoint(server.host(), server.port())),
                1, 1);

        Message response = client.attempt();
        assertNotNull(response);
        LookupResult result = LookupResult.fromResponse(response);

        assertTrue(result.isAccepted());
        assertEquals(server.port(), result.primaryPort());
    }

    @Test
    void lookupAdvanceCyclesEndpoints() {
        SessionConfig config = new SessionConfig().compId("TEST");
        LookupClient client = new LookupClient(dict, config,
                List.of(new LookupClient.Endpoint("a", 1), new LookupClient.Endpoint("b", 2)),
                1, 1);

        assertEquals("a", client.currentEndpoint().host());
        assertEquals("b", client.advance().host());
        assertEquals("a", client.advance().host());
    }
}
