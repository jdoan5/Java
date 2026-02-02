package com.johndoan.helpdesk.api.dto;

import com.johndoan.helpdesk.domain.Priority;
import com.johndoan.helpdesk.domain.TicketStatus;

public class TicketResponse {
    private long id;
    private String title;
    private String description;
    private Priority priority;
    private TicketStatus status;

    public TicketResponse(long id, String title, String description, Priority priority, TicketStatus status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.status = status;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public TicketStatus getStatus() { return status; }
}