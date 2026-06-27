package com.johndoan.bookmarks.service;

import com.johndoan.bookmarks.domain.Bookmark;
import com.johndoan.bookmarks.repository.BookmarkRepository;
import com.johndoan.bookmarks.web.NotFoundException;
import com.johndoan.bookmarks.web.dto.CreateBookmarkRequest;
import com.johndoan.bookmarks.web.dto.UpdateBookmarkRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for bookmarks. Every method takes the {@code owner} (the JWT
 * subject of the caller) and only ever touches that owner's data — so two users
 * never see or modify each other's bookmarks.
 */
@Service
@Transactional
public class BookmarkService {

    private final BookmarkRepository repository;

    public BookmarkService(BookmarkRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Bookmark> list(String owner, String tag) {
        if (tag != null && !tag.isBlank()) {
            return repository.findByOwnerAndTag(owner, tag.trim().toLowerCase());
        }
        return repository.findByOwnerOrderByIdAsc(owner);
    }

    @Transactional(readOnly = true)
    public Bookmark get(Long id, String owner) {
        return repository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new NotFoundException("Bookmark " + id + " not found"));
    }

    public Bookmark create(CreateBookmarkRequest request, String owner) {
        Instant now = Instant.now();
        Bookmark bookmark = new Bookmark(
                null,
                owner,
                request.title(),
                request.url(),
                normalizeTags(request.tags()),
                request.notes(),
                now,
                now
        );
        return repository.save(bookmark);
    }

    public Bookmark update(Long id, UpdateBookmarkRequest request, String owner) {
        Bookmark existing = get(id, owner); // throws NotFoundException if missing or not owned
        existing.setTitle(request.title());
        existing.setUrl(request.url());
        existing.setTags(normalizeTags(request.tags()));
        existing.setNotes(request.notes());
        existing.setUpdatedAt(Instant.now());
        return repository.save(existing);
    }

    public void delete(Long id, String owner) {
        Bookmark existing = get(id, owner); // 404 if missing or owned by someone else
        repository.delete(existing);
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return new ArrayList<>();
        }
        List<String> cleaned = new ArrayList<>();
        for (String tag : tags) {
            if (tag != null && !tag.isBlank()) {
                cleaned.add(tag.trim().toLowerCase());
            }
        }
        return cleaned;
    }
}
