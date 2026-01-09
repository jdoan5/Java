package com.johndoan.helpdesk.service;

import com.johndoan.helpdesk.domain.Priority;
import com.johndoan.helpdesk.domain.Ticket;
import com.johndoan.helpdesk.domain.TicketStatus;
import com.johndoan.helpdesk.repo.TicketRepository;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public final class TicketService {
    private final TicketRepository repo;
    private final AtomicLong idSeq = new AtomicLong(1000);

    public TicketService(TicketRepository repo) {
        this.repo = repo;
    }

    public Ticket create(String title, String description, Priority priority) {
        if (title == null || title.trim().isEmpty()) throw new IllegalArgumentException("Title is required.");
        if (description == null || description.trim().isEmpty()) throw new IllegalArgumentException("Description is required.");

        Ticket t = new Ticket(idSeq.incrementAndGet(), title, description, priority);
        return repo.save(t);
    }

    public List<Ticket> listAll() {
        return repo.findAll();
    }

    public Ticket getOrThrow(long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + id));
    }

    public Ticket updateStatus(long id, TicketStatus newStatus) {
        Ticket t = getOrThrow(id);

        // Simple workflow rule example (Stage 1):
        // CLOSED can only happen from RESOLVED.
        if (newStatus == TicketStatus.CLOSED && t.getStatus() != TicketStatus.RESOLVED) {
            throw new IllegalStateException("Ticket must be RESOLVED before it can be CLOSED.");
        }

        t.setStatus(newStatus);
        return repo.save(t);
    }

    public void delete(long id) {
        // Ensure it exists (clear feedback)
        getOrThrow(id);
        repo.deleteById(id);
    }

    public List<Ticket> search(String query) {
        if (query == null) query = "";
        final String q = query.trim().toLowerCase(Locale.ROOT);

        if (q.isEmpty()) return listAll();

        return repo.findAll().stream()
                .filter(t ->
                        t.getTitle().toLowerCase(Locale.ROOT).contains(q) ||
                                t.getDescription().toLowerCase(Locale.ROOT).contains(q) ||
                                t.getStatus().name().toLowerCase(Locale.ROOT).contains(q) ||
                                t.getPriority().name().toLowerCase(Locale.ROOT).contains(q) ||
                                String.valueOf(t.getId()).contains(q)
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                )
                .collect(Collectors.toList());
    }
}