package com.auction.app.domains.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {

    @NotBlank(message = "Feedback content cannot be empty")
    @Pattern(
            regexp = "^(?:\\s*\\S+){1,250}\\s*$",
            message = "Feedback content must not exceed 250 words"
    )
    private String content;

}
