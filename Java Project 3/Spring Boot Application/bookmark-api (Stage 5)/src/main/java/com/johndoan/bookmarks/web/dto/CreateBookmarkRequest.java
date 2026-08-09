package com.johndoan.bookmarks.web.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import java.util.List;

/**
 * Incoming JSON for "create a bookmark".
 *
 * A Java record is perfect for an immutable request payload. The validation
 * annotations are checked automatically because the controller marks the
 * parameter with {@code @Valid}.
 */
public record CreateBookmarkRequest(

        @NotBlank(message = "title is required")
        String title,

        @NotBlank(message = "url is required")
        @URL(message = "url must be a valid URL")
        String url,

        List<String> tags,

        String notes
) {
}
