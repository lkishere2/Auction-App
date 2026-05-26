package com.auction.app.feedback;

import com.auction.app.domains.feedback.FeedbackController;
import com.auction.app.domains.feedback.FeedbackService;
import com.auction.app.domains.feedback.dtos.FeedbackRequest;
import com.auction.app.domains.feedback.dtos.FeedbackResponse;
import com.auction.app.infrastructure.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = FeedbackController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeedbackService feedbackService;

    // --- POST /api/v1/feedback ---

    @Test
    void createFeedback_WhenRequestIsValid_ShouldReturnCreated() throws Exception {
        // Arrange
        FeedbackRequest request = createFeedbackRequest("Great auction experience!");
        FeedbackResponse response = createFeedbackResponse(1L, "Great auction experience!");

        when(feedbackService.createFeedback(any(FeedbackRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.content").value("Great auction experience!"));

        verify(feedbackService).createFeedback(any(FeedbackRequest.class));
    }

    @Test
    void createFeedback_WhenContentIsBlank_ShouldReturnBadRequest() throws Exception {
        // Arrange
        FeedbackRequest request = createFeedbackRequest("");

        // Act & Assert
        mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedbackService);
    }

    @Test
    void createFeedback_WhenContentIsNull_ShouldReturnBadRequest() throws Exception {
        // Arrange
        FeedbackRequest request = new FeedbackRequest(); // content = null

        // Act & Assert
        mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedbackService);
    }

    // --- PUT /api/v1/feedback/{id} ---

    @Test
    void updateFeedback_WhenRequestIsValid_ShouldReturnOk() throws Exception {
        // Arrange
        FeedbackRequest request = createFeedbackRequest("Updated feedback content");
        FeedbackResponse response = createFeedbackResponse(1L, "Updated feedback content");

        when(feedbackService.updateFeedback(eq(1L), any(FeedbackRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/v1/feedback/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.content").value("Updated feedback content"));

        verify(feedbackService).updateFeedback(eq(1L), any(FeedbackRequest.class));
    }

    @Test
    void updateFeedback_WhenContentIsBlank_ShouldReturnBadRequest() throws Exception {
        // Arrange
        FeedbackRequest request = createFeedbackRequest("");

        // Act & Assert
        mockMvc.perform(put("/api/v1/feedback/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedbackService);
    }

    @Test
    void updateFeedback_WhenContentIsNull_ShouldReturnBadRequest() throws Exception {
        // Arrange
        FeedbackRequest request = new FeedbackRequest(); // content = null

        // Act & Assert
        mockMvc.perform(put("/api/v1/feedback/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(feedbackService);
    }

    // --- DELETE /api/v1/feedback/{id} ---

    @Test
    void deleteFeedback_WhenIdExists_ShouldReturnNoContent() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/v1/feedback/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(feedbackService).deleteFeedback(1L);
    }

    // --- GET /api/v1/feedback/my ---

    @Test
    void getCurrentUserFeedback_WhenCalledWithValidParams_ShouldReturnSlice() throws Exception {
        // Arrange
        FeedbackResponse feedbackResponse = createFeedbackResponse(1L, "My feedback");
        Slice<FeedbackResponse> mockSlice = new SliceImpl<>(List.of(feedbackResponse));

        when(feedbackService.getCurrentUserFeedback(0, 10)).thenReturn(mockSlice);

        // Act & Assert
        mockMvc.perform(get("/api/v1/feedback/my")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].content").value("My feedback"));

        verify(feedbackService).getCurrentUserFeedback(0, 10);
    }

    @Test
    void getCurrentUserFeedback_WhenParamsAreMissing_ShouldUseDefaults() throws Exception {
        // Arrange
        Slice<FeedbackResponse> mockSlice = new SliceImpl<>(List.of());
        when(feedbackService.getCurrentUserFeedback(0, 10)).thenReturn(mockSlice);

        // Act & Assert
        mockMvc.perform(get("/api/v1/feedback/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(feedbackService).getCurrentUserFeedback(0, 10);
    }

    // --- GET /api/v1/feedback (ADMIN) ---

    @Test
    void getAllFeedback_WhenCalledWithValidParams_ShouldReturnPage() throws Exception {
        FeedbackResponse feedbackResponse = createFeedbackResponse(1L, "Some feedback");
        Page<FeedbackResponse> mockPage = new PageImpl<>(List.of(feedbackResponse));
        when(feedbackService.getAllFeedback(0, 20)).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/feedback/all")  // ← sửa chỗ này
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].content").value("Some feedback"));

        verify(feedbackService).getAllFeedback(0, 20);
    }

    @Test
    void getAllFeedback_WhenParamsAreMissing_ShouldUseDefaults() throws Exception {
        Page<FeedbackResponse> mockPage = new PageImpl<>(List.of());
        when(feedbackService.getAllFeedback(0, 20)).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/feedback/all"))  // ← sửa chỗ này
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(feedbackService).getAllFeedback(0, 20);
    }
    // --- Helper Methods ---

    private FeedbackRequest createFeedbackRequest(String content) {
        FeedbackRequest request = new FeedbackRequest();
        request.setContent(content);
        return request;
    }

    private FeedbackResponse createFeedbackResponse(Long id, String content) {
        return FeedbackResponse.builder()
                .id(id)
                .username("testuser")
                .email("test@example.com")
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
