package com.johndoan.helpdesk.repo;

import com.johndoan.helpdesk.domain.Ticket;

import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    Ticket save(Ticket ticket);
    Optional<Ticket> findById(long id);
    List<Ticket> findAll();
    void deleteById(long id);
}