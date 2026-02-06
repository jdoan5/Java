package com.johndoan.helpdesk.api;

import com.johndoan.helpdesk.api.dto.CreateTicketRequest;
import com.johndoan.helpdesk.api.dto.TicketResponse;
import com.johndoan.helpdesk.api.dto.UpdateTicketRequest;
import com.johndoan.helpdesk.domain.Ticket;
import com.johndoan.helpdesk.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketMapper ticketMapper;

    public TicketController(TicketService ticketService, TicketMapper ticketMapper) {
        this.ticketService = ticketService;
        this.ticketMapper = ticketMapper;
    }

    @GetMapping
    public List<TicketResponse> getAll() {
        return ticketService.getAllTickets().stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public TicketResponse getById(@PathVariable long id) {
        return ticketMapper.toResponse(ticketService.getTicketById(id));
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@RequestBody CreateTicketRequest req) {
        Ticket created = ticketService.createTicket(req.getTitle(), req.getDescription(), req.getPriority());
        TicketResponse body = ticketMapper.toResponse(created);

        return ResponseEntity
                .created(URI.create("/api/tickets/" + created.getId()))
                .body(body);
    }

    @PutMapping("/{id}")
    public TicketResponse replace(@PathVariable long id, @RequestBody UpdateTicketRequest req) {
        Ticket updated = ticketService.updateTicket(
                id,
                req.getTitle(),
                req.getDescription(),
                req.getPriority(),
                req.getStatus()
        );
        return ticketMapper.toResponse(updated);
    }

    @PatchMapping("/{id}")
    public TicketResponse patch(@PathVariable long id, @RequestBody UpdateTicketRequest req) {
        Ticket updated = ticketService.patchTicket(
                id,
                req.getTitle(),
                req.getDescription(),
                req.getPriority(),
                req.getStatus()
        );
        return ticketMapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        ticketService.deleteTicket(id);
    }
}