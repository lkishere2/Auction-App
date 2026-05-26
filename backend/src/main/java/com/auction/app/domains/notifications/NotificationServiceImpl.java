package com.auction.app.domains.notifications;

import com.auction.app.domains.notifications.model.Notification;
import com.auction.app.domains.notifications.model.NotificationType;
import com.auction.app.domains.users.connection.ConnectionRepository;
import com.auction.app.domains.users.users.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public Slice<NotificationResponse> getNotificationsFeed(long receiverId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sendAt").descending());

        return notificationRepository.findByReceiverId(receiverId, pageable)
                .map(entity -> new NotificationResponse(
                        entity.getMessage(),
                        entity.getSendAt()
                ));
    }

    @Override
    @Async("notificationExecutor")
    public void createAndSend(User receiver, User sender, NotificationType type) {
        String message = type.generateMessage(sender.getDisplayName());

        Notification notification = Notification.builder()
                .receiver(receiver)
                .sender(sender)
                .notificationType(type)
                .message(message)
                .build();
        notificationRepository.save(notification);

        NotificationResponse response = new NotificationResponse(message, notification.getSendAt());

        messagingTemplate.convertAndSendToUser(
                receiver.getDisplayName(),
                "/queue/notifications",
                response
        );
        log.info("{} has sent a notification to {}", sender.getDisplayName(), receiver.getDisplayName());
    }

    @Override
    @Async("notificationExecutor")
    @Transactional
    public void notifyFollowersOfNewAuction(User creator) {
        log.info("Processing bulk follower notification for {} on thread: {}",
                creator.getDisplayName(), Thread.currentThread().getName());

        List<User> followers = connectionRepository.findAllFollowersByFollowingId(creator.getId());
        if (followers.isEmpty()) return;

        String message = NotificationType.NEW_AUCTION.generateMessage(creator.getDisplayName());
        LocalDateTime now = LocalDateTime.now();

        List<Notification> bulkNotifications = followers.stream()
                .map(follower -> Notification.builder()
                        .receiver(follower)
                        .sender(creator)
                        .notificationType(NotificationType.NEW_AUCTION)
                        .message(message)
                        .build())
                .toList();

        notificationRepository.saveAll(bulkNotifications);

        NotificationResponse response = new NotificationResponse(message, now);
        for (User follower : followers) {
            messagingTemplate.convertAndSendToUser(
                    follower.getDisplayName(),
                    "/queue/notifications",
                    response
            );
        }

        log.info("Bulk notifications completed for {} followers.", followers.size());
    }
}
