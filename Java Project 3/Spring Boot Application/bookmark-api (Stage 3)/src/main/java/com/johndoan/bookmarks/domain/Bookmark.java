package com.johndoan.bookmarks.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Core domain object, now a JPA {@code @Entity} mapped to the {@code bookmarks}
 * table. Hibernate reads these annotations to create the schema and to convert
 * between rows and objects.
 *
 * JPA requires a no-arg constructor and non-final fields, which is why this is a
 * mutable class with getters/setters rather than a record.
 */
@Entity
@Table(name = "bookmarks")
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String url;

    /**
     * A list of simple String values needs its own table. EAGER keeps it simple
     * for this small app (the tags load with the bookmark, no lazy-loading
     * surprises when we map to a DTO).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "bookmark_tags", joinColumns = @JoinColumn(name = "bookmark_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Column(length = 2000)
    private String notes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
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
