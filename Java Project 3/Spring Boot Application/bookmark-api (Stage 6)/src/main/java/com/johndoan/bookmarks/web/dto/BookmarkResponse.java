package com.johndoan.bookmarks.web.dto;

import com.johndoan.bookmarks.domain.Bookmark;

import java.time.Instant;
import java.util.List;

/**
 * Outgoing JSON shape. Includes {@code owner} so you can see which identity a
 * bookmark belongs to when comparing the app token vs a logged-in user.
 */
public record BookmarkResponse(
        Long id,
        String owner,
        String title,
        String url,
        List<String> tags,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static BookmarkResponse from(Bookmark b) {
        return new BookmarkResponse(
                b.getId(),
                b.getOwner(),
                b.getTitle(),
                b.getUrl(),
                b.getTags(),
                b.getNotes(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}
