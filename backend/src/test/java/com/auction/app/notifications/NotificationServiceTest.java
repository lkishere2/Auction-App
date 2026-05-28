package com.auction.app.notifications;

import com.auction.app.domains.notifications.NotificationRepository;
import com.auction.app.domains.notifications.NotificationServiceImpl;
import com.auction.app.domains.notifications.dtos.NotificationResponse;
import com.auction.app.domains.notifications.model.Notification;
import com.auction.app.domains.notifications.model.NotificationType;
import com.auction.app.domains.users.connection.ConnectionRepository;
import com.auction.app.domains.users.users.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ConnectionRepository connectionRepository;

    private CapturingSimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User receiver;
    private User sender;
    private User creator;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "notificationRepository", notificationRepository);
        ReflectionTestUtils.setField(notificationService, "connectionRepository", connectionRepository);
        messagingTemplate = new CapturingSimpMessagingTemplate();
        ReflectionTestUtils.setField(notificationService, "messagingTemplate", messagingTemplate);

        receiver = createUser(1L, "receiver", "receiver@example.com");
        sender = createUser(2L, "sender", "sender@example.com");
        creator = createUser(3L, "creator", "creator@example.com");
    }

    @Test
    void getNotificationsFeed_WhenNotificationsExist_ShouldReturnMappedResponses() {
        Notification notification = createNotification(receiver, sender, NotificationType.FOLLOWING, "sender has followed you!", 9);
        when(notificationRepository.findByReceiverId(eq(1L), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(notification)));

        Slice<NotificationResponse> result = notificationService.getNotificationsFeed(1L, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getMessage()).isEqualTo("sender has followed you!");
        assertThat(result.getContent().get(0).getSendAt()).isEqualTo(notification.getSendAt());
    }

    @Test
    void getNotificationsFeed_WhenCalled_ShouldUseReceiverIdAndPageableSortedBySendAtDesc() {
        when(notificationRepository.findByReceiverId(eq(1L), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of()));

        notificationService.getNotificationsFeed(1L, 2, 5);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findByReceiverId(eq(1L), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("sendAt")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("sendAt").isDescending()).isTrue();
    }

    @Test
    void getNotificationsFeed_WhenNoNotifications_ShouldReturnEmptySlice() {
        when(notificationRepository.findByReceiverId(eq(1L), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of()));

        Slice<NotificationResponse> result = notificationService.getNotificationsFeed(1L, 0, 20);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void getNotificationsFeed_WhenSliceHasNextTrue_ShouldPreserveHasNext() {
        Notification notification = createNotification(receiver, sender, NotificationType.FOLLOWING, "message", 9);
        Slice<Notification> slice = new SliceImpl<>(
                List.of(notification),
                PageRequest.of(0, 1),
                true
        );
        when(notificationRepository.findByReceiverId(eq(1L), any(Pageable.class))).thenReturn(slice);

        Slice<NotificationResponse> result = notificationService.getNotificationsFeed(1L, 0, 1);

        assertThat(result.hasNext()).isTrue();
        assertThat(result.isLast()).isFalse();
    }

    @Test
    void getNotificationsFeed_WhenPageOrSizeIsInvalid_ShouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> notificationService.getNotificationsFeed(1L, -1, 20))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> notificationService.getNotificationsFeed(1L, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(notificationRepository);
    }

    @Test
    void getNotificationsFeed_WhenMultipleNotifications_ShouldMapAllResponsesInOrder() {
        Notification first = createNotification(receiver, sender, NotificationType.FOLLOWING, "first", 11);
        Notification second = createNotification(receiver, sender, NotificationType.NEW_AUCTION, "second", 10);
        Notification third = createNotification(receiver, sender, NotificationType.FOLLOWING, "third", 9);
        when(notificationRepository.findByReceiverId(eq(1L), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(first, second, third)));

        Slice<NotificationResponse> result = notificationService.getNotificationsFeed(1L, 0, 20);

        assertThat(result.getContent()).extracting(NotificationResponse::getMessage)
                .containsExactly("first", "second", "third");
    }

    @Test
    void createAndSend_WhenFollowingType_ShouldSaveNotificationWithGeneratedMessage() {
        notificationService.createAndSend(receiver, sender, NotificationType.FOLLOWING);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification saved = notificationCaptor.getValue();
        assertThat(saved.getReceiver()).isEqualTo(receiver);
        assertThat(saved.getSender()).isEqualTo(sender);
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.FOLLOWING);
        assertThat(saved.getMessage()).isEqualTo("sender has followed you!");
    }

    @Test
    void createAndSend_WhenNewAuctionType_ShouldSaveNotificationWithGeneratedMessage() {
        notificationService.createAndSend(receiver, sender, NotificationType.NEW_AUCTION);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());

        Notification saved = notificationCaptor.getValue();
        assertThat(saved.getNotificationType()).isEqualTo(NotificationType.NEW_AUCTION);
        assertThat(saved.getMessage()).isEqualTo("sender has created a new auction, stay tuned!");
    }

    @Test
    void createAndSend_WhenNotificationCreated_ShouldSendWebSocketToReceiverDisplayName() {
        notificationService.createAndSend(receiver, sender, NotificationType.FOLLOWING);

        assertThat(messagingTemplate.sentMessages).hasSize(1);
        SentMessage sentMessage = messagingTemplate.sentMessages.get(0);
        assertThat(sentMessage.user).isEqualTo("receiver");
        assertThat(sentMessage.destination).isEqualTo("/queue/notifications");
        assertThat(sentMessage.payload.getMessage()).isEqualTo("sender has followed you!");
    }

    @Test
    void createAndSend_WhenNotificationCreated_ShouldUseNotificationSendAtInResponse() {
        notificationService.createAndSend(receiver, sender, NotificationType.FOLLOWING);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);

        verify(notificationRepository).save(notificationCaptor.capture());

        assertThat(messagingTemplate.sentMessages).hasSize(1);
        assertThat(messagingTemplate.sentMessages.get(0).payload.getSendAt())
                .isEqualTo(notificationCaptor.getValue().getSendAt());
    }

    @Test
    void createAndSend_WhenSenderIsNull_ShouldThrowNullPointerException() {
        assertThatThrownBy(() -> notificationService.createAndSend(receiver, null, NotificationType.FOLLOWING))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(notificationRepository);
        assertThat(messagingTemplate.sentMessages).isEmpty();
    }

    @Test
    void createAndSend_WhenReceiverIsNull_ShouldThrowNullPointerException() {
        assertThatThrownBy(() -> notificationService.createAndSend(null, sender, NotificationType.FOLLOWING))
                .isInstanceOf(NullPointerException.class);

        verify(notificationRepository).save(any(Notification.class));
        assertThat(messagingTemplate.sentMessages).isEmpty();
    }

    @Test
    void createAndSend_WhenTypeIsNull_ShouldThrowNullPointerException() {
        assertThatThrownBy(() -> notificationService.createAndSend(receiver, sender, null))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(notificationRepository);
        assertThat(messagingTemplate.sentMessages).isEmpty();
    }

    @Test
    void notifyFollowersOfNewAuction_WhenCreatorHasNoFollowers_ShouldNotSaveOrSend() {
        when(connectionRepository.findAllFollowersByFollowingId(3L)).thenReturn(List.of());

        notificationService.notifyFollowersOfNewAuction(creator);

        verify(notificationRepository, never()).saveAll(anyList());
        assertThat(messagingTemplate.sentMessages).isEmpty();
    }

    @Test
    void notifyFollowersOfNewAuction_WhenCreatorHasFollowers_ShouldSaveNotificationForEachFollower() {
        User firstFollower = createUser(4L, "firstFollower", "first@example.com");
        User secondFollower = createUser(5L, "secondFollower", "second@example.com");
        when(connectionRepository.findAllFollowersByFollowingId(3L)).thenReturn(List.of(firstFollower, secondFollower));

        notificationService.notifyFollowersOfNewAuction(creator);

        ArgumentCaptor<List<Notification>> notificationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(notificationsCaptor.capture());

        List<Notification> savedNotifications = notificationsCaptor.getValue();
        assertThat(savedNotifications).hasSize(2);
        assertThat(savedNotifications).extracting(Notification::getReceiver)
                .containsExactly(firstFollower, secondFollower);
        assertThat(savedNotifications).allSatisfy(notification -> {
            assertThat(notification.getSender()).isEqualTo(creator);
            assertThat(notification.getNotificationType()).isEqualTo(NotificationType.NEW_AUCTION);
        });
    }

    @Test
    void notifyFollowersOfNewAuction_WhenCreatorHasFollowers_ShouldSendWebSocketToEveryFollower() {
        User firstFollower = createUser(4L, "firstFollower", "first@example.com");
        User secondFollower = createUser(5L, "secondFollower", "second@example.com");
        when(connectionRepository.findAllFollowersByFollowingId(3L)).thenReturn(List.of(firstFollower, secondFollower));

        notificationService.notifyFollowersOfNewAuction(creator);

        assertThat(messagingTemplate.sentMessages).extracting(SentMessage::user)
                .containsExactly("firstFollower", "secondFollower");
        assertThat(messagingTemplate.sentMessages).allSatisfy(sentMessage ->
                assertThat(sentMessage.destination).isEqualTo("/queue/notifications")
        );
    }

    @Test
    void notifyFollowersOfNewAuction_WhenCreatorHasFollowers_ShouldUseNewAuctionMessage() {
        User follower = createUser(4L, "follower", "follower@example.com");
        when(connectionRepository.findAllFollowersByFollowingId(3L)).thenReturn(List.of(follower));

        notificationService.notifyFollowersOfNewAuction(creator);

        ArgumentCaptor<List<Notification>> notificationsCaptor = ArgumentCaptor.forClass(List.class);

        verify(notificationRepository).saveAll(notificationsCaptor.capture());

        assertThat(notificationsCaptor.getValue().get(0).getMessage())
                .isEqualTo("creator has created a new auction, stay tuned!");
        assertThat(messagingTemplate.sentMessages).hasSize(1);
        assertThat(messagingTemplate.sentMessages.get(0).payload.getMessage())
                .isEqualTo("creator has created a new auction, stay tuned!");
    }

    @Test
    void notifyFollowersOfNewAuction_WhenCreatorIsNull_ShouldThrowNullPointerException() {
        assertThatThrownBy(() -> notificationService.notifyFollowersOfNewAuction(null))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(connectionRepository, notificationRepository);
        assertThat(messagingTemplate.sentMessages).isEmpty();
    }

    private Notification createNotification(
            User receiver,
            User sender,
            NotificationType type,
            String message,
            int hour
    ) {
        return Notification.builder()
                .receiver(receiver)
                .sender(sender)
                .notificationType(type)
                .message(message)
                .sendAt(LocalDateTime.of(2026, 5, 27, hour, 0))
                .build();
    }

    private User createUser(Long id, String username, String email) {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .password("password")
                .enabled(true)
                .build();
    }

    private static class CapturingSimpMessagingTemplate extends SimpMessagingTemplate {
        private final List<SentMessage> sentMessages = new ArrayList<>();

        CapturingSimpMessagingTemplate() {
            super(new NoOpMessageChannel());
        }

        @Override
        public void convertAndSendToUser(String user, String destination, Object payload) {
            sentMessages.add(new SentMessage(user, destination, (NotificationResponse) payload));
        }
    }

    private record SentMessage(String user, String destination, NotificationResponse payload) {
    }

    private static class NoOpMessageChannel implements MessageChannel {
        @Override
        public boolean send(Message<?> message) {
            return true;
        }

        @Override
        public boolean send(Message<?> message, long timeout) {
            return true;
        }
    }
}
