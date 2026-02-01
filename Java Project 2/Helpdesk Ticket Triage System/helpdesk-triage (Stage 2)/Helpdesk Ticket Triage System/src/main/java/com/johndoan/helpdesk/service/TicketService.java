package com.johndoan.helpdesk.service;

import com.johndoan.helpdesk.domain.Priority;
import com.johndoan.helpdesk.domain.Ticket;
import com.johndoan.helpdesk.domain.TicketStatus;
import com.johndoan.helpdesk.exception.NotFoundException;
import com.johndoan.helpdesk.repo.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final AtomicLong idGenerator = new AtomicLong(0);

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public Ticket getTicketById(long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket not found: " + id));
    }

    public Ticket createTicket(String title, String description, Priority priority) {
        long id = idGenerator.incrementAndGet();
        Ticket ticket = new Ticket(id, title, description, priority, TicketStatus.NEW);
        return ticketRepository.save(ticket);
    }

    // "PUT" semantics: replace full resource
    public Ticket updateTicket(long id, String title, String description, Priority priority, TicketStatus status) {
        Ticket existing = getTicketById(id);
        existing.setTitle(title);
        existing.setDescription(description);
        existing.setPriority(priority);
        existing.setStatus(status == null ? existing.getStatus() : status);
        return ticketRepository.save(existing);
    }

    // "PATCH" semantics: partial update
    public Ticket patchTicket(long id, String title, String description, Priority priority, TicketStatus status) {
        Ticket existing = getTicketById(id);

        if (title != null) existing.setTitle(title);
        if (description != null) existing.setDescription(description);
        if (priority != null) existing.setPriority(priority);
        if (status != null) existing.setStatus(status);

        return ticketRepository.save(existing);
    }

    public void deleteTicket(long id) {
        // fail fast if missing
        getTicketById(id);
        ticketRepository.deleteById(id);
    }
}