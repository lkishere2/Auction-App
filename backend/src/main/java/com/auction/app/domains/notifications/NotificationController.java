package com.auction.app.domains.notifications;

import com.auction.app.domains.notifications.dtos.NotificationResponse;
import com.auction.app.domains.users.users.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/feed")
    public ResponseEntity<Slice<NotificationResponse>> getMyNotificationsFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User currentUser = getCurrentUser();
        Slice<NotificationResponse> feed = notificationService.getNotificationsFeed(
                currentUser.getId(),
                page,
                size
        );
        return ResponseEntity.ok(feed);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Unauthorized: User is not authenticated");
        }
        return (User) authentication.getPrincipal();
    }
}