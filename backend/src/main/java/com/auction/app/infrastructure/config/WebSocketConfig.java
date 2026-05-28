package com.auction.app.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /*
        This method configures how messages are routed between clients and server.
        What it does? Overrides the default broker config to establish custom prefixes
        for incoming and outgoing data.
    */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        /*
            This one configures an in-memory message broker.
            Any destination prefix starts with /topic or /queue should be handled by this broker
            to send data back to clients.
        */
        config.enableSimpleBroker("/topic", "/queue");
        /*

        */
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}

/*
    Conventional:
    /topic is used for Pub/Sub broadcasting while /queue is used for point-to-point private message
*/