package com.johndoan.helpdesk.domain;

import java.util.Objects;

public class Ticket {

    private long id;
    private String title;
    private String description;
    private Priority priority;
    private TicketStatus status;

    // 4-arg constructor (older code expects this)
    public Ticket(long id, String title, String description, Priority priority) {
        this(id, title, description, priority, TicketStatus.NEW);
    }

    // 5-arg constructor (newer code expects this)
    public Ticket(long id, String title, String description, Priority priority, TicketStatus status) {
        this.id = id;
        this.title = requireNonBlank(title, "title");
        this.description = requireNonBlank(description, "description");
        this.priority = Objects.requireNonNull(priority, "priority must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = requireNonBlank(title, "title");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = requireNonBlank(description, "description");
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = Objects.requireNonNull(priority, "priority must not be null");
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    private static String requireNonBlank(String v, String field) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return v.trim();
    }
}