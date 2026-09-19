package com.johndoan.helpdesk.service;

import com.johndoan.helpdesk.domain.Priority;
import com.johndoan.helpdesk.domain.Ticket;
import com.johndoan.helpdesk.domain.TicketStatus;
import com.johndoan.helpdesk.exception.NotFoundException;
import com.johndoan.helpdesk.repo.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Service-layer tests with a mocked repository, so these assert the SERVICE's own
 * rules rather than JPA's: the status a new ticket starts life in, the difference
 * between a full replace and a partial patch, and the not-found guards.
 *
 * <p>The patch null-skipping is the subtlest logic in the class — a "field was not
 * sent" must not be confused with "field was set to null" — so it gets the most
 * coverage here.
 *
 * <p>Every write path asserts on the entity CAPTURED at {@code save(...)}, never on
 * the returned object alone. The service mutates the entity it loaded, so the return
 * value carries the new field values whether or not the save ever happened; only the
 * captured argument proves a write was requested. Each write path also asserts that
 * the caller gets back what the repository returned, not the pre-save entity — the
 * stub deliberately returns a DIFFERENT instance so that distinction can be made.
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    private static final long KNOWN_ID = 7L;
    private static final long MISSING_ID = 4242L;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketService ticketService;

    @Captor
    private ArgumentCaptor<Ticket> savedTicket;

    /** A stored ticket in a known, mid-life state, so a patch has something to leave alone. */
    private static Ticket existingTicket() {
        return new Ticket("Printer jammed", "3rd floor, tray 2", Priority.MEDIUM, TicketStatus.IN_PROGRESS);
    }

    /**
     * Stands in for what {@code JpaRepository.save} hands back — a distinct instance, not the
     * argument, which is what lets a test tell "returned the repository's result" apart from
     * "returned the entity it was about to save".
     */
    private static Ticket repositoryResult() {
        return new Ticket("value returned by save", "value returned by save", Priority.LOW, TicketStatus.NEW);
    }

    @Test
    void createTicketStartsTheTicketInTheNewStatus() {
        Ticket fromRepository = repositoryResult();
        when(ticketRepository.save(any(Ticket.class))).thenReturn(fromRepository);

        Ticket created = ticketService.createTicket("Laptop will not boot", "Dell XPS, no POST", Priority.HIGH);

        verify(ticketRepository).save(savedTicket.capture());
        Ticket persisted = savedTicket.getValue();
        assertThat(persisted.getTitle()).isEqualTo("Laptop will not boot");
        assertThat(persisted.getDescription()).isEqualTo("Dell XPS, no POST");
        assertThat(persisted.getPriority()).isEqualTo(Priority.HIGH);
        // The caller never chooses the opening status; the service pins it to NEW.
        assertThat(persisted.getStatus()).isEqualTo(TicketStatus.NEW);
        // The caller gets the repository's result (which carries the generated id), not the
        // detached instance the service built.
        assertThat(created).isSameAs(fromRepository);
    }

    @Test
    void getTicketByIdReturnsTheStoredTicket() {
        Ticket stored = existingTicket();
        when(ticketRepository.findById(KNOWN_ID)).thenReturn(Optional.of(stored));

        assertThat(ticketService.getTicketById(KNOWN_ID)).isSameAs(stored);
    }

    @Test
    void getTicketByIdThrowsNotFoundNamingTheMissingId() {
        when(ticketRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(MISSING_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(String.valueOf(MISSING_ID));
    }

    @Test
    void updateTicketOverwritesEveryFieldIncludingStatus() {
        Ticket stored = existingTicket();
        Ticket fromRepository = repositoryResult();
        when(ticketRepository.findById(KNOWN_ID)).thenReturn(Optional.of(stored));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(fromRepository);

        Ticket updated = ticketService.updateTicket(
                KNOWN_ID, "Printer replaced", "Swapped for a spare", Priority.LOW, TicketStatus.RESOLVED);

        // The update must be HANDED TO the repository, not merely applied in memory.
        verify(ticketRepository).save(savedTicket.capture());
        Ticket persisted = savedTicket.getValue();
        // The loaded row is updated in place, so the write targets the existing ticket rather
        // than inserting a second one.
        assertThat(persisted).isSameAs(stored);
        assertThat(persisted.getTitle()).isEqualTo("Printer replaced");
        assertThat(persisted.getDescription()).isEqualTo("Swapped for a spare");
        assertThat(persisted.getPriority()).isEqualTo(Priority.LOW);
        assertThat(persisted.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(updated).isSameAs(fromRepository);
    }

    @Test
    void updateTicketThrowsNotFoundAndSavesNothingForAnUnknownId() {
        when(ticketRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.updateTicket(
                MISSING_ID, "Anything", "Anything", Priority.LOW, TicketStatus.NEW))
                .isInstanceOf(NotFoundException.class);

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void patchTicketWithOnlyAStatusLeavesTheOtherFieldsUntouched() {
        Ticket stored = existingTicket();
        Ticket fromRepository = repositoryResult();
        when(ticketRepository.findById(KNOWN_ID)).thenReturn(Optional.of(stored));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(fromRepository);

        // This is exactly the call the Angular client makes: {"status": "RESOLVED"} and nothing else.
        Ticket patched = ticketService.patchTicket(KNOWN_ID, null, null, null, TicketStatus.RESOLVED);

        verify(ticketRepository).save(savedTicket.capture());
        Ticket persisted = savedTicket.getValue();
        assertThat(persisted.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(persisted.getTitle()).isEqualTo("Printer jammed");
        assertThat(persisted.getDescription()).isEqualTo("3rd floor, tray 2");
        assertThat(persisted.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(patched).isSameAs(fromRepository);
    }

    @Test
    void patchTicketWithOnlyAPriorityLeavesTheOtherFieldsUntouched() {
        Ticket stored = existingTicket();
        Ticket fromRepository = repositoryResult();
        when(ticketRepository.findById(KNOWN_ID)).thenReturn(Optional.of(stored));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(fromRepository);

        Ticket patched = ticketService.patchTicket(KNOWN_ID, null, null, Priority.HIGH, null);

        verify(ticketRepository).save(savedTicket.capture());
        Ticket persisted = savedTicket.getValue();
        assertThat(persisted.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(persisted.getTitle()).isEqualTo("Printer jammed");
        assertThat(persisted.getDescription()).isEqualTo("3rd floor, tray 2");
        assertThat(persisted.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(patched).isSameAs(fromRepository);
    }

    @Test
    void patchTicketWithNoFieldsAtAllChangesNothing() {
        Ticket stored = existingTicket();
        Ticket fromRepository = repositoryResult();
        when(ticketRepository.findById(KNOWN_ID)).thenReturn(Optional.of(stored));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(fromRepository);

        // An empty patch body is a no-op, not a request to null every column out.
        Ticket patched = ticketService.patchTicket(KNOWN_ID, null, null, null, null);

        verify(ticketRepository).save(savedTicket.capture());
        Ticket persisted = savedTicket.getValue();
        assertThat(persisted.getTitle()).isEqualTo("Printer jammed");
        assertThat(persisted.getDescription()).isEqualTo("3rd floor, tray 2");
        assertThat(persisted.getPriority()).isEqualTo(Priority.MEDIUM);
        assertThat(persisted.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(patched).isSameAs(fromRepository);
    }

    @Test
    void patchTicketThrowsNotFoundAndSavesNothingForAnUnknownId() {
        when(ticketRepository.findById(MISSING_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.patchTicket(
                MISSING_ID, null, null, null, TicketStatus.CLOSED))
                .isInstanceOf(NotFoundException.class);

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void deleteTicketRemovesAnExistingTicket() {
        when(ticketRepository.existsById(KNOWN_ID)).thenReturn(true);

        ticketService.deleteTicket(KNOWN_ID);

        verify(ticketRepository).deleteById(KNOWN_ID);
    }

    @Test
    void deleteTicketThrowsNotFoundAndDeletesNothingForAnUnknownId() {
        when(ticketRepository.existsById(MISSING_ID)).thenReturn(false);

        assertThatThrownBy(() -> ticketService.deleteTicket(MISSING_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(String.valueOf(MISSING_ID));

        // The existsById guard is the point: a blind deleteById would silently succeed.
        verify(ticketRepository, never()).deleteById(anyLong());
    }
}
