package com.johndoan.bookmarks.service;

import com.johndoan.bookmarks.domain.Bookmark;
import com.johndoan.bookmarks.repository.BookmarkRepository;
import com.johndoan.bookmarks.web.NotFoundException;
import com.johndoan.bookmarks.web.dto.CreateBookmarkRequest;
import com.johndoan.bookmarks.web.dto.UpdateBookmarkRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for bookmarks. The controller stays thin and delegates here;
 * the repository only knows how to store and fetch.
 */
@Service
public class BookmarkService {

    private final BookmarkRepository repository;

    public BookmarkService(BookmarkRepository repository) {
        this.repository = repository;
    }

    public List<Bookmark> list(String tag) {
        if (tag != null && !tag.isBlank()) {
            return repository.findByTag(tag);
        }
        return repository.findAll();
    }

    public Bookmark get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Bookmark " + id + " not found"));
    }

    public Bookmark create(CreateBookmarkRequest request) {
        Instant now = Instant.now();
        Bookmark bookmark = new Bookmark(
                null,
                request.title(),
                request.url(),
                normalizeTags(request.tags()),
                request.notes(),
                now,
                now
        );
        return repository.save(bookmark);
    }

    public Bookmark update(Long id, UpdateBookmarkRequest request) {
        Bookmark existing = get(id); // throws NotFoundException if missing
        existing.setTitle(request.title());
        existing.setUrl(request.url());
        existing.setTags(normalizeTags(request.tags()));
        existing.setNotes(request.notes());
        existing.setUpdatedAt(Instant.now());
        return repository.save(existing);
    }

    public void delete(Long id) {
        if (!repository.deleteById(id)) {
            throw new NotFoundException("Bookmark " + id + " not found");
        }
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
