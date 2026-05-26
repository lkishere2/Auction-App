package com.auction.app.feedback;

import com.auction.app.domains.feedback.model.Feedback;
import com.auction.app.domains.feedback.FeedbackRepository;
import com.auction.app.domains.users.users.model.User;
import com.auction.app.domains.users.users.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
class FeedbackRepositoryTest {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User otherUser;
    private Feedback feedback1;
    private Feedback feedback2;
    private Feedback feedback3;
    private Feedback otherUserFeedback;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .email("testuser@example.com")
                .password("securePassword123")
                .enabled(true)
                .build();
        testUser = userRepository.saveAndFlush(testUser);

        otherUser = User.builder()
                .username("otheruser")
                .email("otheruser@example.com")
                .password("securePassword123")
                .enabled(true)
                .build();
        otherUser = userRepository.saveAndFlush(otherUser);

        feedback1 = Feedback.builder()
                .content("Great auction experience!")
                .client(testUser)
                .build();

        feedback2 = Feedback.builder()
                .content("Very smooth transaction")
                .client(testUser)
                .build();

        feedback3 = Feedback.builder()
                .content("Would definitely use again")
                .client(testUser)
                .build();

        otherUserFeedback = Feedback.builder()
                .content("Should not appear in testUser results")
                .client(otherUser)
                .build();

        feedbackRepository.saveAndFlush(feedback1);
        feedbackRepository.saveAndFlush(feedback2);
        feedbackRepository.saveAndFlush(feedback3);
        feedbackRepository.saveAndFlush(otherUserFeedback);
    }

    @AfterEach
    void tearDown() {
        feedbackRepository.deleteAll();
        userRepository.deleteAll();
    }

    // =========================================================================
    // METHOD: findAllByUser (10 Tests)
    // =========================================================================

    @Test
    void findAllByUser_WhenUserHasFeedbacks_ShouldReturnOnlyUserFeedbacks() {
        Pageable pageable = PageRequest.of(0, 10);

        Slice<Feedback> result = feedbackRepository.findAllByUser(testUser, pageable);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
                .extracting(Feedback::getContent)
                .containsExactlyInAnyOrder(
                        "Great auction experience!",
                        "Very smooth transaction",
                        "Would definitely use again"
                )
                .doesNotContain("Should not appear in testUser results");
    }

    @Test
    void findAllByUser_WhenUserHasNoFeedbacks_ShouldReturnEmptySlice() {
        User emptyUser = User.builder()
                .username("emptyuser")
                .email("emptyuser@example.com")
                .password("securePassword123")
                .enabled(true)
                .build();
        emptyUser = userRepository.saveAndFlush(emptyUser);

        Pageable pageable = PageRequest.of(0, 10);

        Slice<Feedback> result = feedbackRepository.findAllByUser(emptyUser, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void findAllByUser_WhenUserHasExactlyOneFeedback_ShouldReturnSingleResult() {
        Pageable pageable = PageRequest.of(0, 10);

        Slice<Feedback> result = feedbackRepository.findAllByUser(otherUser, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent())
                .isEqualTo("Should not appear in testUser results");
    }

    @Test
    void findAllByUser_WhenPageSizeIsLimited_ShouldReturnRequestedPageSize() {
        Pageable pageable = PageRequest.of(0, 2);

        Slice<Feedback> result = feedbackRepository.findAllByUser(testUser, pageable);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findAllByUser_WhenRequestingSecondPage_ShouldReturnRemainingFeedbacks() {
        Pageable pageable = PageRequest.of(1, 2);

        Slice<Feedback> result = feedbackRepository.findAllByUser(testUser, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findAllByUser_WhenMorePagesExist_ShouldHaveNext() {
        Pageable pageable = PageRequest.of(0, 2);

        Slice<Feedback> result = feedbackRepository.findAllByUser(testUser, pageable);

        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void findAllByUser_WhenOnLastPage_ShouldNotHaveNext() {
        Pageable pageable = PageRequest.of(1, 2);

        Slice<Feedback> result = feedbackRepository.findAllByUser(testUser, pageable);

        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void findAllByUser_WhenPageExceedsAvailableData_ShouldReturnEmptySlice() {
        Pageable pageable = PageRequest.of(99, 10);

        Slice<Feedback> result = feedbackRepository.findAllByUser(testUser, pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void findAllByUser_WhenOtherUserHasFeedbacks_ShouldNotReturnTheirFeedbacks() {
        Pageable pageable = PageRequest.of(0, 10);

        Slice<Feedback> result = feedbackRepository.findAllByUser(testUser, pageable);

        assertThat(result.getContent())
                .extracting(Feedback::getContent)
                .doesNotContain("Should not appear in testUser results");
    }

    @Test
    void findAllByUser_WhenFetchingFeedbacks_ShouldFetchClientAlongside() {
        Pageable pageable = PageRequest.of(0, 10);

        Slice<Feedback> result = feedbackRepository.findAllByUser(testUser, pageable);

        assertThat(result.getContent())
                .allSatisfy(feedback -> {
                    assertThat(feedback.getClient()).isNotNull();
                    assertThat(feedback.getClient().getId()).isEqualTo(testUser.getId());
                });
    }

    @Test
    void findAllByUser_WhenUserIsNull_ShouldReturnEmptySlice() {
        Pageable pageable = PageRequest.of(0, 10);

        Slice<Feedback> result = feedbackRepository.findAllByUser(null, pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
