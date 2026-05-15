package com.auction.app.domains.feedback;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

public interface FeedbackService {
    FeedbackResponse createFeedback(FeedbackRequest request);
    FeedbackResponse updateFeedback(Long id, FeedbackRequest request);
    void deleteFeedback(Long id);
    Slice<FeedbackResponse> getCurrentUserFeedback(int page, int size);
    Page<FeedbackResponse> getAllFeedback(int page, int size);
}