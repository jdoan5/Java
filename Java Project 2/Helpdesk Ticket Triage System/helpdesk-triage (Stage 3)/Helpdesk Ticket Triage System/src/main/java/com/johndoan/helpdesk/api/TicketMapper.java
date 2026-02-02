package com.johndoan.helpdesk.api;

import com.johndoan.helpdesk.api.dto.TicketResponse;
import com.johndoan.helpdesk.domain.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {
    public TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus()
        );
    }
}