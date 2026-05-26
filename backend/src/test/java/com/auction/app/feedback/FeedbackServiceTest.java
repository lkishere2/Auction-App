package com.auction.app.feedback;

import com.auction.app.domains.feedback.model.Feedback;
import com.auction.app.domains.feedback.FeedbackRepository;
import com.auction.app.domains.feedback.FeedbackServiceImpl;
import com.auction.app.domains.feedback.dtos.FeedbackRequest;
import com.auction.app.domains.feedback.dtos.FeedbackResponse;
import com.auction.app.domains.feedback.exceptions.FeedBackNotFoundException;
import com.auction.app.domains.users.users.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private FeedbackServiceImpl feedbackService;

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
                new UsernamePasswordAuthenticationToken(currentUser, null)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // METHOD: createFeedback (3 Tests)
    // =========================================================================

    @Test
    void createFeedback_WhenRequestIsValid_ShouldSaveFeedbackForCurrentUser() {
        FeedbackRequest request = createFeedbackRequest("Great auction experience!");
        when(feedbackRepository.save(any(Feedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackResponse response = feedbackService.createFeedback(request);

        ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(feedbackCaptor.capture());
        Feedback savedFeedback = feedbackCaptor.getValue();

        assertThat(savedFeedback.getClient()).isEqualTo(currentUser);
        assertThat(savedFeedback.getContent()).isEqualTo("Great auction experience!");
        assertThat(response.getContent()).isEqualTo("Great auction experience!");
        assertThat(response.getEmail()).isEqualTo("testuser@example.com");
    }

    @Test
    void createFeedback_WhenCalled_ShouldNotAssignAnotherUserAsClient() {
        FeedbackRequest request = createFeedbackRequest("Great auction experience!");
        when(feedbackRepository.save(any(Feedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        feedbackService.createFeedback(request);

        ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(feedbackCaptor.capture());

        assertThat(feedbackCaptor.getValue().getClient().getId()).isEqualTo(1L);
    }

    @Test
    void createFeedback_WhenSaved_ShouldMapAllFieldsToResponse() {
        FeedbackRequest request = createFeedbackRequest("Great!");
        Feedback savedFeedback = buildFeedback(5L, "Great!", currentUser);

        when(feedbackRepository.save(any(Feedback.class))).thenReturn(savedFeedback);

        FeedbackResponse response = feedbackService.createFeedback(request);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getContent()).isEqualTo("Great!");
        assertThat(response.getEmail()).isEqualTo("testuser@example.com");
    }

    // =========================================================================
    // METHOD: updateFeedback (4 Tests)
    // =========================================================================

    @Test
    void updateFeedback_WhenCurrentUserOwnsFeedback_ShouldUpdateContentAndSave() {
        Feedback existingFeedback = buildFeedback(10L, "Old content", currentUser);
        FeedbackRequest request = createFeedbackRequest("New content");

        when(feedbackRepository.findById(10L)).thenReturn(Optional.of(existingFeedback));
        when(feedbackRepository.save(existingFeedback)).thenReturn(existingFeedback);

        FeedbackResponse response = feedbackService.updateFeedback(10L, request);

        assertThat(existingFeedback.getContent()).isEqualTo("New content");
        assertThat(response.getContent()).isEqualTo("New content");
        verify(feedbackRepository).save(existingFeedback);
    }

    @Test
    void updateFeedback_WhenUpdating_ShouldOnlyUpdateContentNotClient() {
        Feedback existingFeedback = buildFeedback(10L, "Old content", currentUser);

        when(feedbackRepository.findById(10L)).thenReturn(Optional.of(existingFeedback));
        when(feedbackRepository.save(existingFeedback)).thenReturn(existingFeedback);

        feedbackService.updateFeedback(10L, createFeedbackRequest("New content"));

        assertThat(existingFeedback.getClient()).isEqualTo(currentUser);
    }

    @Test
    void updateFeedback_WhenFeedbackDoesNotExist_ShouldThrowFeedBackNotFoundException() {
        when(feedbackRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.updateFeedback(99L, createFeedbackRequest("content")))
                .isInstanceOf(FeedBackNotFoundException.class)
                .hasMessage("Feedback not found");

        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    void updateFeedback_WhenFeedbackBelongsToAnotherUser_ShouldThrowAccessDeniedException() {
        User otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .email("other@example.com")
                .password("password")
                .build();
        Feedback feedback = buildFeedback(10L, "Some content", otherUser);

        when(feedbackRepository.findById(10L)).thenReturn(Optional.of(feedback));

        assertThatThrownBy(() -> feedbackService.updateFeedback(10L, createFeedbackRequest("content")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Unauthorized: You cannot delete this feedback.");

        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    // =========================================================================
    // METHOD: deleteFeedback (3 Tests)
    // =========================================================================

    @Test
    void deleteFeedback_WhenCurrentUserOwnsFeedback_ShouldDeleteFeedback() {
        Feedback feedback = buildFeedback(10L, "Some content", currentUser);
        when(feedbackRepository.findById(10L)).thenReturn(Optional.of(feedback));

        feedbackService.deleteFeedback(10L);

        verify(feedbackRepository).delete(feedback);
    }

    @Test
    void deleteFeedback_WhenFeedbackDoesNotExist_ShouldThrowFeedBackNotFoundException() {
        when(feedbackRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feedbackService.deleteFeedback(99L))
                .isInstanceOf(FeedBackNotFoundException.class)
                .hasMessage("Feedback not found");

        verify(feedbackRepository, never()).delete(any(Feedback.class));
    }

    @Test
    void deleteFeedback_WhenFeedbackBelongsToAnotherUser_ShouldThrowAccessDeniedException() {
        User otherUser = User.builder()
                .id(2L)
                .username("otheruser")
                .email("other@example.com")
                .password("password")
                .build();
        Feedback feedback = buildFeedback(10L, "Some content", otherUser);

        when(feedbackRepository.findById(10L)).thenReturn(Optional.of(feedback));

        assertThatThrownBy(() -> feedbackService.deleteFeedback(10L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Unauthorized: You cannot delete this feedback.");

        verify(feedbackRepository, never()).delete(any(Feedback.class));
    }

    // =========================================================================
    // METHOD: getCurrentUserFeedback (4 Tests)
    // =========================================================================

    @Test
    void getCurrentUserFeedback_WhenValidPageAndSize_ShouldReturnMappedResponsesSortedByCreatedAtDesc() {
        Feedback feedback = buildFeedback(1L, "Great experience!", currentUser);
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Slice<Feedback> mockSlice = new SliceImpl<>(List.of(feedback));

        when(feedbackRepository.findAllByUser(eq(currentUser), eq(expectedPageable)))
                .thenReturn(mockSlice);

        Slice<FeedbackResponse> result = feedbackService.getCurrentUserFeedback(0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("Great experience!");
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("testuser@example.com");
        verify(feedbackRepository).findAllByUser(currentUser, expectedPageable);
    }

    @Test
    void getCurrentUserFeedback_WhenNoFeedbacksExist_ShouldReturnEmptySlice() {
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        when(feedbackRepository.findAllByUser(eq(currentUser), eq(expectedPageable)))
                .thenReturn(new SliceImpl<>(List.of()));

        Slice<FeedbackResponse> result = feedbackService.getCurrentUserFeedback(0, 10);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getCurrentUserFeedback_WhenCalled_ShouldAlwaysSortByCreatedAtDescending() {
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        when(feedbackRepository.findAllByUser(eq(currentUser), eq(expectedPageable)))
                .thenReturn(new SliceImpl<>(List.of()));

        feedbackService.getCurrentUserFeedback(0, 10);

        verify(feedbackRepository).findAllByUser(currentUser, expectedPageable);
    }

    @Test
    void getCurrentUserFeedback_WhenMoreDataExists_ShouldReturnSliceWithHasNext() {
        Pageable expectedPageable = PageRequest.of(0, 2, Sort.by("createdAt").descending());
        List<Feedback> feedbacks = List.of(
                buildFeedback(1L, "Feedback 1", currentUser),
                buildFeedback(2L, "Feedback 2", currentUser)
        );
        Slice<Feedback> mockSlice = new SliceImpl<>(feedbacks, expectedPageable, true);

        when(feedbackRepository.findAllByUser(eq(currentUser), eq(expectedPageable)))
                .thenReturn(mockSlice);

        Slice<FeedbackResponse> result = feedbackService.getCurrentUserFeedback(0, 2);

        assertThat(result.hasNext()).isTrue();
    }

    // =========================================================================
    // METHOD: getAllFeedback (3 Tests)
    // =========================================================================

    @Test
    void getAllFeedback_WhenValidPageAndSize_ShouldReturnMappedResponsesSortedByCreatedAtDesc() {
        Feedback feedback = buildFeedback(1L, "Great experience!", currentUser);
        Pageable expectedPageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        Page<Feedback> mockPage = new PageImpl<>(List.of(feedback));

        when(feedbackRepository.findAll(eq(expectedPageable))).thenReturn(mockPage);

        Page<FeedbackResponse> result = feedbackService.getAllFeedback(0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("Great experience!");
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(feedbackRepository).findAll(expectedPageable);
    }

    @Test
    void getAllFeedback_WhenNoFeedbacksExist_ShouldReturnEmptyPage() {
        Pageable expectedPageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        when(feedbackRepository.findAll(eq(expectedPageable)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<FeedbackResponse> result = feedbackService.getAllFeedback(0, 20);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void getAllFeedback_WhenCalled_ShouldAlwaysSortByCreatedAtDescending() {
        Pageable expectedPageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
        when(feedbackRepository.findAll(eq(expectedPageable)))
                .thenReturn(new PageImpl<>(List.of()));

        feedbackService.getAllFeedback(0, 20);

        verify(feedbackRepository).findAll(expectedPageable);
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private FeedbackRequest createFeedbackRequest(String content) {
        FeedbackRequest request = new FeedbackRequest();
        request.setContent(content);
        return request;
    }

    private Feedback buildFeedback(Long id, String content, User client) {
        return Feedback.builder()
                .id(id)
                .content(content)
                .client(client)
                .build();
    }
}
