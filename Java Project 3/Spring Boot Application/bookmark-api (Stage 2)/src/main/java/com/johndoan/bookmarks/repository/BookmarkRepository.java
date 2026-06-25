package com.johndoan.bookmarks.repository;

import com.johndoan.bookmarks.domain.Bookmark;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory store (a thread-safe Map + an id counter).
 *
 * This mirrors the "in-memory first" learning style from the other projects
 * in this repo. Later stages can replace it with Spring Data JPA without the
 * service or web layers needing to change much.
 */
@Repository
public class BookmarkRepository {

    private final Map<Long, Bookmark> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public List<Bookmark> findAll() {
        List<Bookmark> all = new ArrayList<>(store.values());
        all.sort(Comparator.comparing(Bookmark::getId));
        return all;
    }

    public Optional<Bookmark> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Bookmark> findByTag(String tag) {
        List<Bookmark> matches = new ArrayList<>();
        for (Bookmark b : store.values()) {
            if (b.getTags() != null && b.getTags().contains(tag)) {
                matches.add(b);
            }
        }
        matches.sort(Comparator.comparing(Bookmark::getId));
        return matches;
    }

    /**
     * Insert (when id is null) or update (when id is set) and return the
     * stored object.
     */
    public Bookmark save(Bookmark bookmark) {
        if (bookmark.getId() == null) {
            bookmark.setId(sequence.incrementAndGet());
        }
        store.put(bookmark.getId(), bookmark);
        return bookmark;
    }

    public boolean deleteById(Long id) {
        return store.remove(id) != null;
    }

    public boolean existsById(Long id) {
        return store.containsKey(id);
    }
}
