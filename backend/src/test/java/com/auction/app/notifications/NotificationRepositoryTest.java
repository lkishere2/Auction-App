package com.auction.app.notifications;

import com.auction.app.domains.notifications.NotificationRepository;
import com.auction.app.domains.notifications.model.Notification;
import com.auction.app.domains.notifications.model.NotificationType;
import com.auction.app.domains.users.users.UserRepository;
import com.auction.app.domains.users.users.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private User receiver;
    private User otherReceiver;
    private User sender;

    @BeforeEach
    void setUp() {
        receiver = userRepository.save(createUser("receiver", "receiver@example.com"));
        otherReceiver = userRepository.save(createUser("otherReceiver", "other.receiver@example.com"));
        sender = userRepository.save(createUser("sender", "sender@example.com"));
    }

    @Test
    void findByReceiverId_WhenReceiverHasNotifications_ShouldReturnOnlyThatReceiverNotifications() {
        Notification expected = saveNotification(receiver, sender, NotificationType.FOLLOWING, "sender has followed you!", 9);
        saveNotification(otherReceiver, sender, NotificationType.NEW_AUCTION, "sender has created a new auction, stay tuned!", 10);

        Slice<Notification> result = notificationRepository.findByReceiverId(
                receiver.getId(),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(expected.getId());
        assertThat(result.getContent().get(0).getReceiver().getId()).isEqualTo(receiver.getId());
    }

    @Test
    void findByReceiverId_WhenMultipleNotifications_ShouldReturnAllForReceiver() {
        saveNotification(receiver, sender, NotificationType.FOLLOWING, "sender has followed you!", 9);
        saveNotification(receiver, sender, NotificationType.NEW_AUCTION, "sender has created a new auction, stay tuned!", 10);
        saveNotification(receiver, otherReceiver, NotificationType.FOLLOWING, "otherReceiver has followed you!", 11);

        Slice<Notification> result = notificationRepository.findByReceiverId(
                receiver.getId(),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    void findByReceiverId_WhenNotificationFieldsPersisted_ShouldKeepAllFields() {
        LocalDateTime sendAt = LocalDateTime.of(2026, 5, 27, 9, 30);
        Notification saved = Notification.builder()
                .receiver(receiver)
                .sender(sender)
                .notificationType(NotificationType.FOLLOWING)
                .message("sender has followed you!")
                .sendAt(sendAt)
                .build();
        saved = notificationRepository.save(saved);

        Notification found = notificationRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getReceiver().getId()).isEqualTo(receiver.getId());
        assertThat(found.getSender().getId()).isEqualTo(sender.getId());
        assertThat(found.getNotificationType()).isEqualTo(NotificationType.FOLLOWING);
        assertThat(found.getMessage()).isEqualTo("sender has followed you!");
        assertThat(found.getSendAt()).isEqualTo(sendAt);
    }

    @Test
    void findByReceiverId_WhenPageSizeSmallerThanData_ShouldReturnHasNextTrue() {
        saveNotification(receiver, sender, NotificationType.FOLLOWING, "message 1", 9);
        saveNotification(receiver, sender, NotificationType.FOLLOWING, "message 2", 10);
        saveNotification(receiver, sender, NotificationType.FOLLOWING, "message 3", 11);

        Slice<Notification> result = notificationRepository.findByReceiverId(
                receiver.getId(),
                PageRequest.of(0, 2)
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void findByReceiverId_WhenOnLastPage_ShouldReturnHasNextFalse() {
        saveNotification(receiver, sender, NotificationType.FOLLOWING, "message 1", 9);
        saveNotification(receiver, sender, NotificationType.FOLLOWING, "message 2", 10);

        Slice<Notification> result = notificationRepository.findByReceiverId(
                receiver.getId(),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void findByReceiverId_WhenSecondPageRequested_ShouldReturnCorrectPageContent() {
        Notification first = saveNotification(receiver, sender, NotificationType.FOLLOWING, "message 1", 9);
        Notification second = saveNotification(receiver, sender, NotificationType.FOLLOWING, "message 2", 10);
        Notification third = saveNotification(receiver, sender, NotificationType.FOLLOWING, "message 3", 11);

        Slice<Notification> result = notificationRepository.findByReceiverId(
                receiver.getId(),
                PageRequest.of(1, 2, Sort.by("id").ascending())
        );

        assertThat(result.getContent()).extracting(Notification::getId)
                .containsExactly(third.getId())
                .doesNotContain(first.getId(), second.getId());
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void findByReceiverId_WhenPageExceedsData_ShouldReturnEmptySlice() {
        saveNotification(receiver, sender, NotificationType.FOLLOWING, "message 1", 9);

        Slice<Notification> result = notificationRepository.findByReceiverId(
                receiver.getId(),
                PageRequest.of(99, 10)
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void findByReceiverId_WhenReceiverHasNoNotifications_ShouldReturnEmptySlice() {
        saveNotification(otherReceiver, sender, NotificationType.FOLLOWING, "sender has followed you!", 9);

        Slice<Notification> result = notificationRepository.findByReceiverId(
                receiver.getId(),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void findByReceiverId_WhenReceiverDoesNotExist_ShouldReturnEmptySlice() {
        saveNotification(receiver, sender, NotificationType.FOLLOWING, "sender has followed you!", 9);

        Slice<Notification> result = notificationRepository.findByReceiverId(
                999L,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void findByReceiverId_WhenSenderIdMatchesButReceiverIdDoesNot_ShouldNotReturnBySenderId() {
        saveNotification(otherReceiver, receiver, NotificationType.FOLLOWING, "receiver has followed you!", 9);

        Slice<Notification> result = notificationRepository.findByReceiverId(
                receiver.getId(),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findByReceiverId_WhenReceiverIdIsZeroOrNegative_ShouldReturnEmptySlice() {
        saveNotification(receiver, sender, NotificationType.FOLLOWING, "sender has followed you!", 9);

        Slice<Notification> zeroIdResult = notificationRepository.findByReceiverId(0L, PageRequest.of(0, 10));
        Slice<Notification> negativeIdResult = notificationRepository.findByReceiverId(-1L, PageRequest.of(0, 10));

        assertThat(zeroIdResult.getContent()).isEmpty();
        assertThat(negativeIdResult.getContent()).isEmpty();
    }

    @Test
    void findByReceiverId_WhenSortedBySendAtDescending_ShouldReturnNewestFirst() {
        Notification oldest = saveNotification(receiver, sender, NotificationType.FOLLOWING, "oldest", 9);
        Notification newest = saveNotification(receiver, sender, NotificationType.NEW_AUCTION, "newest", 11);
        Notification middle = saveNotification(receiver, sender, NotificationType.FOLLOWING, "middle", 10);

        Slice<Notification> result = notificationRepository.findByReceiverId(
                receiver.getId(),
                PageRequest.of(0, 10, Sort.by("sendAt").descending())
        );

        assertThat(result.getContent()).extracting(Notification::getId)
                .containsExactly(newest.getId(), middle.getId(), oldest.getId());
    }

    private Notification saveNotification(
            User receiver,
            User sender,
            NotificationType type,
            String message,
            int hour
    ) {
        return notificationRepository.save(Notification.builder()
                .receiver(receiver)
                .sender(sender)
                .notificationType(type)
                .message(message)
                .sendAt(LocalDateTime.of(2026, 5, 27, hour, 0))
                .build());
    }

    private User createUser(String username, String email) {
        return User.builder()
                .username(username)
                .email(email)
                .password("password")
                .enabled(true)
                .build();
    }
}
