package com.johndoan.bookmarks.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Core domain object: a saved link.
 *
 * Kept as a plain mutable class (not a record) because the in-memory
 * repository updates fields in place. Later stages may swap this for a
 * JPA @Entity backed by a database.
 */
public class Bookmark {

    private Long id;
    private String title;
    private String url;
    private List<String> tags = new ArrayList<>();
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public Bookmark() {
    }

    public Bookmark(Long id, String title, String url, List<String> tags, String notes,
                    Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.tags = (tags != null) ? new ArrayList<>(tags) : new ArrayList<>();
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = (tags != null) ? new ArrayList<>(tags) : new ArrayList<>();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
