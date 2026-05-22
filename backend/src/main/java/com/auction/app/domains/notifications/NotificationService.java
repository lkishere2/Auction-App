package com.auction.app.domains.notifications;

import com.auction.app.domains.users.users.User;
import org.springframework.data.domain.Slice;

public interface NotificationService {
    Slice<NotificationResponse> getNotificationsFeed(long receiverId, int page, int size);
    void createAndSend(User receiver, User sender, NotificationType type);
    void notifyFollowersOfNewAuction(User creator);
}
