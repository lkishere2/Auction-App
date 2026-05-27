package com.auction.app.notifications;

import com.auction.app.domains.notifications.NotificationController;
import com.auction.app.domains.notifications.NotificationService;
import com.auction.app.domains.notifications.dtos.NotificationResponse;
import com.auction.app.domains.users.users.model.User;
import com.auction.app.infrastructure.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = NotificationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("testuser@example.com")
                .password("password")
                .enabled(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyNotificationsFeed_WhenValidParams_ShouldReturnSliceWithData() throws Exception {
        LocalDateTime sendAt = LocalDateTime.of(2026, 5, 27, 9, 0);
        NotificationResponse response = new NotificationResponse("testuser has followed you!", sendAt);
        Slice<NotificationResponse> slice = new SliceImpl<>(List.of(response));

        when(notificationService.getNotificationsFeed(1L, 0, 20)).thenReturn(slice);

        mockMvc.perform(get("/api/v1/notifications/feed")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].message").value("testuser has followed you!"))
                .andExpect(jsonPath("$.content[0].sendAt").value("2026-05-27T09:00:00"));

        verify(notificationService).getNotificationsFeed(1L, 0, 20);
    }

    @Test
    void getMyNotificationsFeed_WhenParamsAreMissing_ShouldUseDefaultPageAndSize() throws Exception {
        Slice<NotificationResponse> slice = new SliceImpl<>(List.of());

        when(notificationService.getNotificationsFeed(1L, 0, 20)).thenReturn(slice);

        mockMvc.perform(get("/api/v1/notifications/feed"))
                .andExpect(status().isOk());

        verify(notificationService).getNotificationsFeed(1L, 0, 20);
    }

    @Test
    void getMyNotificationsFeed_WhenCustomParams_ShouldPassCustomPageAndSizeToService() throws Exception {
        Slice<NotificationResponse> slice = new SliceImpl<>(List.of());

        when(notificationService.getNotificationsFeed(1L, 1, 10)).thenReturn(slice);

        mockMvc.perform(get("/api/v1/notifications/feed")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(notificationService).getNotificationsFeed(1L, 1, 10);
    }

    @Test
    void getMyNotificationsFeed_WhenDataReturned_ShouldMapMessageCorrectly() throws Exception {
        String expectedMessage = "khanhkaiser has followed you!";
        NotificationResponse response = new NotificationResponse(expectedMessage, LocalDateTime.of(2026, 5, 27, 10, 0));

        when(notificationService.getNotificationsFeed(1L, 0, 20)).thenReturn(new SliceImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/notifications/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].message").value(expectedMessage));
    }

    @Test
    void getMyNotificationsFeed_WhenMultipleNotifications_ShouldReturnAllNotifications() throws Exception {
        List<NotificationResponse> responses = List.of(
                new NotificationResponse("user1 has followed you!", LocalDateTime.of(2026, 5, 27, 9, 0)),
                new NotificationResponse("user2 has created a new auction, stay tuned!", LocalDateTime.of(2026, 5, 27, 9, 5)),
                new NotificationResponse("user3 has followed you!", LocalDateTime.of(2026, 5, 27, 9, 10))
        );

        when(notificationService.getNotificationsFeed(1L, 0, 20)).thenReturn(new SliceImpl<>(responses));

        mockMvc.perform(get("/api/v1/notifications/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].message").value("user1 has followed you!"))
                .andExpect(jsonPath("$.content[1].message").value("user2 has created a new auction, stay tuned!"))
                .andExpect(jsonPath("$.content[2].message").value("user3 has followed you!"));
    }

    @Test
    void getMyNotificationsFeed_WhenNoNotificationsExist_ShouldReturnEmptySlice() throws Exception {
        when(notificationService.getNotificationsFeed(1L, 0, 20)).thenReturn(new SliceImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/notifications/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getMyNotificationsFeed_WhenPageExceedsData_ShouldReturnEmptySlice() throws Exception {
        when(notificationService.getNotificationsFeed(1L, 99, 20)).thenReturn(new SliceImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/notifications/feed")
                        .param("page", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(notificationService).getNotificationsFeed(1L, 99, 20);
    }

    @Test
    void getMyNotificationsFeed_WhenMoreDataExists_ShouldReturnLastFalse() throws Exception {
        Slice<NotificationResponse> slice = new SliceImpl<>(
                List.of(new NotificationResponse("msg", LocalDateTime.of(2026, 5, 27, 11, 0))),
                PageRequest.of(0, 1),
                true
        );

        when(notificationService.getNotificationsFeed(1L, 0, 1)).thenReturn(slice);

        mockMvc.perform(get("/api/v1/notifications/feed")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void getMyNotificationsFeed_WhenNoMoreDataExists_ShouldReturnLastTrue() throws Exception {
        Slice<NotificationResponse> slice = new SliceImpl<>(
                List.of(new NotificationResponse("msg", LocalDateTime.of(2026, 5, 27, 11, 30))),
                PageRequest.of(0, 10),
                false
        );

        when(notificationService.getNotificationsFeed(1L, 0, 10)).thenReturn(slice);

        mockMvc.perform(get("/api/v1/notifications/feed")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getMyNotificationsFeed_WhenAuthenticationIsNull_ShouldReturn500() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/notifications/feed"))
                .andExpect(status().is5xxServerError());

        verifyNoInteractions(notificationService);
    }

    @Test
    void getMyNotificationsFeed_WhenAuthenticationIsNotAuthenticated_ShouldReturn500() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null)
        );

        mockMvc.perform(get("/api/v1/notifications/feed"))
                .andExpect(status().is5xxServerError());

        verifyNoInteractions(notificationService);
    }
}
