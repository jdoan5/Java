package com.johndoan.helpdesk.domain;

import java.time.Instant;
import java.util.Objects;

public final class Ticket {
    private final long id;
    private String title;
    private String description;
    private Priority priority;
    private TicketStatus status;
    private final Instant createdAt;

    public Ticket(long id, String title, String description, Priority priority) {
        this.id = id;
        this.title = Objects.requireNonNull(title).trim();
        this.description = Objects.requireNonNull(description).trim();
        this.priority = Objects.requireNonNull(priority);
        this.status = TicketStatus.OPEN;
        this.createdAt = Instant.now();
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public TicketStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setTitle(String title) { this.title = Objects.requireNonNull(title).trim(); }
    public void setDescription(String description) { this.description = Objects.requireNonNull(description).trim(); }
    public void setPriority(Priority priority) { this.priority = Objects.requireNonNull(priority); }
    public void setStatus(TicketStatus status) { this.status = Objects.requireNonNull(status); }

    @Override
    public String toString() {
        return "Ticket{" +
                "id=" + id +
                ", status=" + status +
                ", priority=" + priority +
                ", title='" + title + '\'' +
                '}';
    }
}