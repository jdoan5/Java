package com.johndoan.helpdesk.repo;

import com.johndoan.helpdesk.domain.Ticket;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTicketRepository implements TicketRepository {

    private final ConcurrentHashMap<Long, Ticket> store = new ConcurrentHashMap<>();

    @Override
    public Ticket save(Ticket ticket) {
        store.put(ticket.getId(), ticket);
        return ticket;
    }

    @Override
    public Optional<Ticket> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Ticket> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteById(long id) {
        store.remove(id);
    }
}