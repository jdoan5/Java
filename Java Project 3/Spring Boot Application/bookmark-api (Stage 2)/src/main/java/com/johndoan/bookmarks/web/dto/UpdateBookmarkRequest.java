package com.johndoan.bookmarks.web.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

import java.util.List;

/**
 * Incoming JSON for "replace an existing bookmark" (full update / PUT).
 */
public record UpdateBookmarkRequest(

        @NotBlank(message = "title is required")
        String title,

        @NotBlank(message = "url is required")
        @URL(message = "url must be a valid URL")
        String url,

        List<String> tags,

        String notes
) {
}
