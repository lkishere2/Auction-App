package com.auction.app.domains.notifications.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NotificationResponse {
    private String message;
    private LocalDateTime sendAt;
}
