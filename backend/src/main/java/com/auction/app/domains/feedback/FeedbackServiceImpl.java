package com.auction.app.domains.feedback;

import com.auction.app.domains.feedback.dtos.FeedbackRequest;
import com.auction.app.domains.feedback.dtos.FeedbackResponse;
import com.auction.app.domains.feedback.exceptions.FeedBackNotFoundException;
import com.auction.app.domains.feedback.model.Feedback;
import com.auction.app.domains.users.users.model.User;
import com.auction.app.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public FeedbackResponse createFeedback(FeedbackRequest request) {
        Feedback feedback = mapToEntity(request);
        return mapToResponse(feedbackRepository.save(feedback));
    }

    @Override
    @Transactional
    public FeedbackResponse updateFeedback(Long id, FeedbackRequest request) {
        Feedback feedback = findAndValidateCurrentUser(id);
        feedback.setContent(request.getContent());
        return mapToResponse(feedbackRepository.save(feedback));
    }

    @Override
    @Transactional
    public void deleteFeedback(Long id) {
        Feedback feedback = findAndValidateCurrentUser(id);
        feedbackRepository.delete(feedback);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<FeedbackResponse> getCurrentUserFeedback(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Slice<Feedback> feedbackSlice = feedbackRepository.findAllByUser(currentUser(), pageable);
        return feedbackSlice.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FeedbackResponse> getAllFeedback(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return feedbackRepository.findAll(pageable).map(this::mapToResponse);
    }

    // Helpers
    private User currentUser() {
        try {
            return securityUtils.getCurrentUser();
        } catch (IllegalStateException e) {
            throw new BadCredentialsException("User session is invalid or expired.", e);
        }
    }

    private Feedback findAndValidateCurrentUser(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new FeedBackNotFoundException("Feedback not found"));

        try {
            if (!feedback.getClient().getId().equals(securityUtils.getCurrentUserId())) {
                throw new AccessDeniedException("Unauthorized: You cannot delete this feedback.");
            }
        } catch (IllegalStateException e) {
            throw new BadCredentialsException("User session is invalid or expired.", e);
        }

        return feedback;
    }

    private Feedback mapToEntity(FeedbackRequest request) {
        return Feedback.builder()
                .content(request.getContent())
                .client(currentUser())
                .build();
    }

    private FeedbackResponse mapToResponse(Feedback feedback) {
        User client = feedback.getClient();
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .username(client.getDisplayName())
                .email(client.getEmail())
                .content(feedback.getContent())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}