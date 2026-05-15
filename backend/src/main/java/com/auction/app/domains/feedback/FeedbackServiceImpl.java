package com.auction.app.domains.feedback;

import com.auction.app.domains.users.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackServiceImpl implements FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Override
    @Transactional
    public FeedbackResponse createFeedback(FeedbackRequest request) {
        Feedback feedback = new Feedback();
        feedback.setContent(request.getContent());
        feedback.setClient(getCurrentUser());

        return mapToResponse(feedbackRepository.save(feedback));
    }

    @Override
    @Transactional
    public FeedbackResponse updateFeedback(Long id, FeedbackRequest request) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));

        User currentUser = getCurrentUser();
        if (!feedback.getClient().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized: You do not own this feedback.");
        }

        feedback.setContent(request.getContent());
        return mapToResponse(feedbackRepository.save(feedback));
    }

    @Override
    @Transactional
    public void deleteFeedback(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found"));

        User currentUser = getCurrentUser();
        if (!feedback.getClient().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Unauthorized: You cannot delete this feedback.");
        }

        feedbackRepository.delete(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<FeedbackResponse> getCurrentUserFeedback(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Slice<Feedback> feedbackSlice = feedbackRepository.findAllByUser(getCurrentUser(), pageable);

        return feedbackSlice.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getAllFeedback(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return feedbackRepository.findAll(pageable).map(this::mapToResponse);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    private FeedbackResponse mapToResponse(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getClient().getDisplayName(),
                feedback.getClient().getEmail(),
                feedback.getContent(),
                feedback.getCreatedAt()
        );
    }
}