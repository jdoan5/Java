package com.johndoan.helpdesk.repo;

import com.johndoan.helpdesk.domain.Ticket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryTicketRepository implements TicketRepository {
    private final Map<Long, Ticket> store = new ConcurrentHashMap<>();

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
        List<Ticket> all = new ArrayList<>(store.values());
        all.sort(Comparator.comparingLong(Ticket::getId));
        return all;
    }

    @Override
    public void deleteById(long id) {
        store.remove(id);
    }
}