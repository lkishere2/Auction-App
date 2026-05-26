package com.auction.app.connection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketConnectionTest {

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;
    private String webSocketUrl;

    @BeforeEach
    void setUp() {
        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        this.webSocketUrl = String.format("ws://localhost:%d/ws", port);
    }

    @Test
    void testConnection() throws ExecutionException, InterruptedException, TimeoutException {

        // Use a CompletableFuture to safely bridge the asynchronous WS handshake thread with our test thread
        CompletableFuture<StompSession> completableFuture = new CompletableFuture<>();

        // Act: Attempt to connect to the STOMP broker endpoint
        stompClient.connectAsync(webSocketUrl, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                // Triggered automatically as soon as the upgrade handshake succeeds
                completableFuture.complete(session);
            }
        });

        // Block for a maximum of 3 seconds waiting for the handshake confirmation
        StompSession stompSession = completableFuture.get(3, TimeUnit.SECONDS);

        // Assert: Verify the session is created, alive, and connected
        assertNotNull(stompSession);
        assertTrue(stompSession.isConnected());

        // Clean up: Disconnect gracefully when done
        stompSession.disconnect();
    }
}
