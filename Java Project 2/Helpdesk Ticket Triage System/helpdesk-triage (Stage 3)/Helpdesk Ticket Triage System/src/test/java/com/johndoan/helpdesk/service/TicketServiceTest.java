package com.johndoan.helpdesk.service;

import com.johndoan.helpdesk.domain.Priority;
import com.johndoan.helpdesk.domain.Ticket;
import com.johndoan.helpdesk.domain.TicketStatus;
import com.johndoan.helpdesk.exception.NotFoundException;
import com.johndoan.helpdesk.repo.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Service-layer tests with the repository mocked, so no database, no Spring
 * context and no HTTP is involved — only the triage rules themselves.
 *
 * <p>The subtlest logic here is the difference between {@code updateTicket}
 * (full replace: every field is written, including nulls) and
 * {@code patchTicket} (partial update: null means "leave this alone").
 * The Angular front end depends on the patch behaviour, so it is pinned
 * from several angles below.
 *
 * <p>Every success path that is supposed to write ends in
 * {@code verify(ticketRepository).save(...)} and asserts the captured entity.
 * Asserting only on the object the service returns is not enough: the service
 * mutates the entity in place, so the returned object carries the new values
 * whether or not {@code save} was ever called. The negative paths keep their
 * {@code never()} verifications, and the two together are what make a dropped
 * {@code save} a real assertion failure rather than an incidental Mockito
 * UnnecessaryStubbing complaint that a lenient strictness setting would erase.
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketService ticketService;

    /** A ticket in its post-triage state, so a patch that wrongly resets a field is visible. */
    private static Ticket existingTicket() {
        return new Ticket("Printer jam", "Tray 2 keeps jamming", Priority.LOW, TicketStatus.IN_PROGRESS);
    }

    /** Captures the single entity handed to {@code save}, failing if none was. */
    private Ticket captureSavedTicket() {
        ArgumentCaptor<Ticket> saved = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void createTicketPassesTheSubmittedFieldsThroughToTheSavedEntity() {
        ticketService.createTicket("VPN down", "Cannot reach the gateway", Priority.HIGH);

        Ticket saved = captureSavedTicket();

        assertThat(saved.getTitle()).isEqualTo("VPN down");
        assertThat(saved.getDescription()).isEqualTo("Cannot reach the gateway");
        assertThat(saved.getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    void createTicketAlwaysStampsTheNewStatus() {
        // Every ticket must enter the queue as NEW; the entry point of triage is
        // not something a caller gets to choose.
        ticketService.createTicket("VPN down", "Cannot reach the gateway", Priority.HIGH);

        assertThat(captureSavedTicket().getStatus()).isEqualTo(TicketStatus.NEW);
    }

    @Test
    void createTicketReturnsThePersistedEntityNotTheTransientOne() {
        // The persisted instance is the one carrying the generated id, so returning
        // the locally built ticket instead would hand the caller an id of null.
        Ticket persisted = existingTicket();
        when(ticketRepository.save(any(Ticket.class))).thenReturn(persisted);

        Ticket result = ticketService.createTicket("VPN down", "Cannot reach the gateway", Priority.HIGH);

        assertThat(result).isSameAs(persisted);
    }

    @Test
    void getTicketByIdReturnsTheTicketWhenItExists() {
        Ticket existing = existingTicket();
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(existing));

        assertThat(ticketService.getTicketById(7L)).isSameAs(existing);
    }

    @Test
    void getTicketByIdThrowsNotFoundNamingTheMissingId() {
        // The message reaches the client through GlobalExceptionHandler, so the
        // id it names is part of the API contract, not just a log line.
        when(ticketRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getTicketById(42L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Ticket not found: 42");
    }

    @Test
    void getAllTicketsReturnsEveryTicketUnfiltered() {
        Ticket first = existingTicket();
        Ticket second = new Ticket("Laptop swap", "Battery swollen", Priority.MEDIUM, TicketStatus.NEW);
        when(ticketRepository.findAll()).thenReturn(List.of(first, second));

        assertThat(ticketService.getAllTickets()).containsExactly(first, second);
    }

    @Test
    void updateTicketOverwritesEveryFieldAndSavesTheResult() {
        Ticket existing = existingTicket();
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(call -> call.getArgument(0));

        Ticket result = ticketService.updateTicket(
                7L, "Printer replaced", "Swapped for a new unit", Priority.HIGH, TicketStatus.RESOLVED);

        // The assertions that matter are on the entity that reached the
        // repository: the returned object was mutated in place and would carry
        // the new values even if the service never saved it.
        Ticket saved = captureSavedTicket();
        assertThat(saved.getTitle()).isEqualTo("Printer replaced");
        assertThat(saved.getDescription()).isEqualTo("Swapped for a new unit");
        assertThat(saved.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.RESOLVED);

        assertThat(result.getTitle()).isEqualTo("Printer replaced");
        assertThat(result.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    }

    @Test
    void updateTicketReturnsWhatTheRepositorySavedNotTheDetachedEntity() {
        // save() on a detached entity is a merge: the managed copy it returns is
        // the one the caller must get back. Returning the argument instead would
        // hand the controller an object that is not attached to anything.
        Ticket existing = existingTicket();
        Ticket persisted = existingTicket();
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(ticketRepository.save(existing)).thenReturn(persisted);

        Ticket result = ticketService.updateTicket(
                7L, "Printer replaced", "Swapped for a new unit", Priority.HIGH, TicketStatus.RESOLVED);

        assertThat(result).isSameAs(persisted);
    }

    @Test
    void updateTicketWritesANullDescriptionThroughToSave() {
        // A unit-level check of the service contract, NOT of any API behaviour:
        // full-replace semantics mean a null argument overwrites the stored value,
        // which is exactly what makes updateTicket unsuitable for the partial
        // updates the UI sends. It is only reachable with the repository mocked --
        // Ticket.description is @Column(nullable = false), so a real PUT with no
        // description fails at the database rather than storing a null.
        Ticket existing = existingTicket();
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(call -> call.getArgument(0));

        ticketService.updateTicket(
                7L, "Printer replaced", null, Priority.HIGH, TicketStatus.RESOLVED);

        assertThat(captureSavedTicket().getDescription()).isNull();
    }

    @Test
    void updateTicketThrowsNotFoundAndSavesNothingForAnUnknownId() {
        when(ticketRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.updateTicket(
                42L, "Anything", "Anything", Priority.LOW, TicketStatus.NEW))
                .isInstanceOf(NotFoundException.class);

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void patchTicketWithOnlyAStatusLeavesTheOtherFieldsUntouched() {
        // The core partial-update contract: the Angular front end PATCHes
        // {"status": "RESOLVED"} and nothing else, and must not lose the ticket text.
        Ticket existing = existingTicket();
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(call -> call.getArgument(0));

        Ticket result = ticketService.patchTicket(7L, null, null, null, TicketStatus.RESOLVED);

        Ticket saved = captureSavedTicket();
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(saved.getTitle()).isEqualTo("Printer jam");
        assertThat(saved.getDescription()).isEqualTo("Tray 2 keeps jamming");
        assertThat(saved.getPriority()).isEqualTo(Priority.LOW);

        assertThat(result.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(result.getTitle()).isEqualTo("Printer jam");
    }

    @Test
    void patchTicketWithEveryArgumentNullChangesNothing() {
        Ticket existing = existingTicket();
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(call -> call.getArgument(0));

        Ticket result = ticketService.patchTicket(7L, null, null, null, null);

        Ticket saved = captureSavedTicket();
        assertThat(saved.getTitle()).isEqualTo("Printer jam");
        assertThat(saved.getDescription()).isEqualTo("Tray 2 keeps jamming");
        assertThat(saved.getPriority()).isEqualTo(Priority.LOW);
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

        assertThat(result.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void patchTicketAppliesEveryFieldThatIsSupplied() {
        // The mirror of the null-skipping tests: skipping nulls must not turn into
        // skipping everything.
        Ticket existing = existingTicket();
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(call -> call.getArgument(0));

        Ticket result = ticketService.patchTicket(
                7L, "Printer replaced", "Swapped for a new unit", Priority.HIGH, TicketStatus.RESOLVED);

        Ticket saved = captureSavedTicket();
        assertThat(saved.getTitle()).isEqualTo("Printer replaced");
        assertThat(saved.getDescription()).isEqualTo("Swapped for a new unit");
        assertThat(saved.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.RESOLVED);

        assertThat(result.getTitle()).isEqualTo("Printer replaced");
        assertThat(result.getStatus()).isEqualTo(TicketStatus.RESOLVED);
    }

    @Test
    void patchTicketReturnsWhatTheRepositorySavedNotTheDetachedEntity() {
        // Same contract as the PUT path: the caller gets the merged instance back.
        Ticket existing = existingTicket();
        Ticket persisted = existingTicket();
        when(ticketRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(ticketRepository.save(existing)).thenReturn(persisted);

        Ticket result = ticketService.patchTicket(7L, null, null, null, TicketStatus.RESOLVED);

        assertThat(result).isSameAs(persisted);
    }

    @Test
    void patchTicketThrowsNotFoundForAnUnknownIdInsteadOfCreatingOne() {
        when(ticketRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.patchTicket(42L, "Anything", null, null, null))
                .isInstanceOf(NotFoundException.class);

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void deleteTicketRemovesTheTicketWhenItExists() {
        when(ticketRepository.existsById(7L)).thenReturn(true);

        ticketService.deleteTicket(7L);

        verify(ticketRepository).deleteById(7L);
    }

    @Test
    void deleteTicketThrowsNotFoundAndDeletesNothingWhenTheTicketIsMissing() {
        when(ticketRepository.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> ticketService.deleteTicket(42L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Ticket not found: 42");

        verify(ticketRepository, never()).deleteById(anyLong());
    }
}
