package com.auction.app.domains.feedback;

import com.auction.app.domains.feedback.exceptions.FeedBackNotFoundException;
import com.auction.app.domains.users.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
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

    //Helpers
    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }

    private Feedback findAndValidateCurrentUser(Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new FeedBackNotFoundException("Feedback not found"));

        if (!feedback.getClient().getId().equals(currentUser().getId())) {
            throw new AccessDeniedException("Unauthorized: You cannot delete this feedback.");
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