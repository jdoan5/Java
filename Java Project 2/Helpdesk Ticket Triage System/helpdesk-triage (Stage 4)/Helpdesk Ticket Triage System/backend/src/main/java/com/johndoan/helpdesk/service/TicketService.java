package com.johndoan.helpdesk.service;

import com.johndoan.helpdesk.domain.Priority;
import com.johndoan.helpdesk.domain.Ticket;
import com.johndoan.helpdesk.domain.TicketStatus;
import com.johndoan.helpdesk.exception.NotFoundException;
import com.johndoan.helpdesk.repo.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

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
        Ticket ticket = new Ticket(title, description, priority, TicketStatus.NEW); // change OPEN if needed
        return ticketRepository.save(ticket);
    }

    public Ticket updateTicket(long id, String title, String description, Priority priority, TicketStatus status) {
        Ticket existing = getTicketById(id);

        existing.setTitle(title);
        existing.setDescription(description);
        existing.setPriority(priority);
        existing.setStatus(status);

        return ticketRepository.save(existing);
    }

    public Ticket patchTicket(long id, String title, String description, Priority priority, TicketStatus status) {
        Ticket existing = getTicketById(id);

        if (title != null) existing.setTitle(title);
        if (description != null) existing.setDescription(description);
        if (priority != null) existing.setPriority(priority);
        if (status != null) existing.setStatus(status);

        return ticketRepository.save(existing);
    }

    public void deleteTicket(long id) {
        if (!ticketRepository.existsById(id)) {
            throw new NotFoundException("Ticket not found: " + id);
        }
        ticketRepository.deleteById(id);
    }
}