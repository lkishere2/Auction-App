package com.auction.app.domains.auction.auction.notification;

import com.auction.app.domains.auction.bids.dtos.BidResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.auction.app.domains.auction.bids.dtos.BidNotificationPayload;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(@NonNull Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());

            if (channel.endsWith(":bids")) {
                subscribeToHistoryChannel(message, channel);
            }
            else {
                subscribeToAuctionChannel(message, channel);
            }
        } catch (Exception e) {
            log.error("Failed to process Redis pub/sub message: {}", e.getMessage());
        }
    }

    private void subscribeToAuctionChannel(Message message, String channel) throws IOException {
        BidNotificationPayload payload = objectMapper.readValue(message.getBody(), BidNotificationPayload.class);
        String destination = "/topic/auction/" + payload.getAuctionId();
        messagingTemplate.convertAndSend(destination, payload);

        log.info("WebSocket push → {} — price ${}", destination, payload.getCurrentPrice());
    }

    private void subscribeToHistoryChannel(Message message, String channel) throws IOException {
        List<BidResponse> history = objectMapper.readValue(
                message.getBody(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, BidResponse.class)
        );
        Long auctionId = extractAuctionId(channel);
        messagingTemplate.convertAndSend("/topic/auction/" + auctionId + "/bids", history);
        log.info("WebSocket push → /topic/auction/{}/bids — {} bids", auctionId, history.size());
    }

    private Long extractAuctionId(String channel) {
        String[] parts = channel.split(":");
        return Long.valueOf(parts[2]);
    }
}