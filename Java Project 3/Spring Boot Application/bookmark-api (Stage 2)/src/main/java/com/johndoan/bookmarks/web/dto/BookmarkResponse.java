package com.johndoan.bookmarks.web.dto;

import com.johndoan.bookmarks.domain.Bookmark;

import java.time.Instant;
import java.util.List;

/**
 * Outgoing JSON shape. Keeping a dedicated response record (instead of
 * returning the domain object directly) means the API contract is decoupled
 * from internal storage details.
 */
public record BookmarkResponse(
        Long id,
        String title,
        String url,
        List<String> tags,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    /** Map a domain object to its API representation. */
    public static BookmarkResponse from(Bookmark b) {
        return new BookmarkResponse(
                b.getId(),
                b.getTitle(),
                b.getUrl(),
                b.getTags(),
                b.getNotes(),
                b.getCreatedAt(),
                b.getUpdatedAt()
        );
    }
}
