package com.johndoan.helpdesk.service;

import com.johndoan.helpdesk.domain.Priority;
import com.johndoan.helpdesk.domain.Ticket;
import com.johndoan.helpdesk.domain.TicketStatus;
import com.johndoan.helpdesk.exception.NotFoundException;
import com.johndoan.helpdesk.repo.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Service-layer tests for Stage 2.
 *
 * The repository is mocked, which buys two things a real repository cannot: each test gets
 * a fresh {@code idGenerator} (the service is not the shared singleton here), and the call
 * to the repository can be asserted directly - {@code verify(...).save(...)} on the success
 * paths, {@code verify(..., never())} on the paths that must write nothing.
 *
 * THIS CLASS IS WHERE THE WRITE IS PINNED. Stage 2's repository stores the Ticket instance
 * itself and the service mutates the instance it loaded, so at the HTTP layer the stored
 * object and the mutated object are the same object and dropping the {@code save} call
 * changes no response - see the note in TicketControllerTest. Against a mock there is no
 * aliasing to hide behind: if {@code updateTicket} or {@code patchTicket} stops calling
 * {@code save}, the {@code verify} on its success path fails. Those verifies are the only
 * thing on this stage that holds "an update is written through the repository", so do not
 * delete one as redundant.
 *
 * Note that {@code save} returns its argument in this stage, so any test that exercises
 * a code path reaching the repository stubs it to behave that way.
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository);
    }

    @Test
    void createTicketAssignsSequentialIdsStartingAtOne() {
        saveReturnsItsArgument();

        assertEquals(1L, ticketService.createTicket("First", "d", Priority.LOW).getId());
        assertEquals(2L, ticketService.createTicket("Second", "d", Priority.LOW).getId());
        assertEquals(3L, ticketService.createTicket("Third", "d", Priority.LOW).getId());
    }

    @Test
    void createTicketDefaultsStatusToNewAndPersistsTheTicket() {
        saveReturnsItsArgument();

        Ticket created = ticketService.createTicket("Printer jam", "Tray 2 keeps jamming", Priority.HIGH);

        assertEquals(TicketStatus.NEW, created.getStatus());

        // The ticket handed to the repository carries the same state that was returned.
        Ticket saved = captureSaved();
        assertEquals("Printer jam", saved.getTitle());
        assertEquals("Tray 2 keeps jamming", saved.getDescription());
        assertEquals(Priority.HIGH, saved.getPriority());
        assertEquals(TicketStatus.NEW, saved.getStatus());
    }

    @Test
    void createTicketTrimsSurroundingWhitespace() {
        saveReturnsItsArgument();

        Ticket created = ticketService.createTicket("   Padded   ", "   spaced out   ", Priority.MEDIUM);

        assertEquals("Padded", created.getTitle());
        assertEquals("spaced out", created.getDescription());
    }

    @Test
    void createTicketRejectsBlankTitleAndSavesNothing() {
        assertThrows(IllegalArgumentException.class,
                () -> ticketService.createTicket(null, "d", Priority.LOW));
        assertThrows(IllegalArgumentException.class,
                () -> ticketService.createTicket("", "d", Priority.LOW));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ticketService.createTicket("   ", "d", Priority.LOW));
        assertEquals("title must not be blank", ex.getMessage());

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void createTicketRejectsBlankDescriptionAndSavesNothing() {
        // CreateTicketRequest does NOT mark description @NotBlank, so bean validation lets a
        // blank one through and only this domain guard stops it. That is why adding @Valid to
        // the controller cannot on its own make the create endpoint safe.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ticketService.createTicket("Has a title", "   ", Priority.LOW));

        assertEquals("description must not be blank", ex.getMessage());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void createTicketRejectsNullPriorityAndSavesNothing() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> ticketService.createTicket("Has a title", "d", null));

        assertEquals("priority must not be null", ex.getMessage());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void getTicketByIdThrowsNotFoundNamingTheId() {
        when(ticketRepository.findById(42L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> ticketService.getTicketById(42L));

        // The controller test asserts this exact string reaches the JSON body.
        assertEquals("Ticket not found: 42", ex.getMessage());
    }

    @Test
    void updateTicketReplacesEveryFieldAndWritesItThroughTheRepository() {
        Ticket stored = new Ticket(7L, "Old title", "Old description", Priority.LOW, TicketStatus.NEW);
        stubExisting(7L, stored);
        saveReturnsItsArgument();

        Ticket updated = ticketService.updateTicket(
                7L, "New title", "New description", Priority.HIGH, TicketStatus.CLOSED);

        assertEquals("New title", updated.getTitle());
        assertEquals("New description", updated.getDescription());
        assertEquals(Priority.HIGH, updated.getPriority());
        assertEquals(TicketStatus.CLOSED, updated.getStatus());

        Ticket saved = captureSaved();
        assertSame(stored, saved, "the loaded ticket is the one written back");
        assertEquals("New title", saved.getTitle());
        assertEquals("New description", saved.getDescription());
        assertEquals(Priority.HIGH, saved.getPriority());
        assertEquals(TicketStatus.CLOSED, saved.getStatus());
    }

    @Test
    void updateTicketKeepsTheExistingStatusWhenStatusIsNullAndWritesItThroughTheRepository() {
        stubExisting(7L, new Ticket(7L, "Old title", "Old description", Priority.LOW, TicketStatus.IN_PROGRESS));
        saveReturnsItsArgument();

        Ticket updated = ticketService.updateTicket(
                7L, "New title", "New description", Priority.HIGH, null);

        // UpdateTicketRequest marks status @NotNull, so now that PUT is validated this branch
        // can no longer be reached over HTTP. The service contract still has it, so it is
        // pinned here rather than in the controller test.
        assertEquals(TicketStatus.IN_PROGRESS, updated.getStatus());
        assertEquals("New title", updated.getTitle());

        Ticket saved = captureSaved();
        assertEquals(TicketStatus.IN_PROGRESS, saved.getStatus());
        assertEquals("New title", saved.getTitle());
    }

    @Test
    void updateTicketThrowsNotFoundForAMissingId() {
        when(ticketRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ticketService.updateTicket(
                404L, "New title", "New description", Priority.HIGH, TicketStatus.CLOSED));

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void patchTicketWithOnlyAStatusLeavesEveryOtherFieldAloneAndWritesItThroughTheRepository() {
        stubExisting(7L, new Ticket(7L, "Keyboard broken", "Space bar sticks", Priority.MEDIUM, TicketStatus.NEW));
        saveReturnsItsArgument();

        Ticket patched = ticketService.patchTicket(7L, null, null, null, TicketStatus.RESOLVED);

        assertEquals(TicketStatus.RESOLVED, patched.getStatus());
        assertEquals("Keyboard broken", patched.getTitle());
        assertEquals("Space bar sticks", patched.getDescription());
        assertEquals(Priority.MEDIUM, patched.getPriority());

        Ticket saved = captureSaved();
        assertEquals(TicketStatus.RESOLVED, saved.getStatus());
        assertEquals("Keyboard broken", saved.getTitle());
        assertEquals("Space bar sticks", saved.getDescription());
        assertEquals(Priority.MEDIUM, saved.getPriority());
    }

    @Test
    void patchTicketWithOnlyATitleLeavesEveryOtherFieldAloneAndWritesItThroughTheRepository() {
        stubExisting(7L, new Ticket(7L, "Keyboard broken", "Space bar sticks", Priority.MEDIUM, TicketStatus.IN_PROGRESS));
        saveReturnsItsArgument();

        Ticket patched = ticketService.patchTicket(7L, "Keyboard replaced", null, null, null);

        assertEquals("Keyboard replaced", patched.getTitle());
        assertEquals("Space bar sticks", patched.getDescription());
        assertEquals(Priority.MEDIUM, patched.getPriority());
        assertEquals(TicketStatus.IN_PROGRESS, patched.getStatus());

        Ticket saved = captureSaved();
        assertEquals("Keyboard replaced", saved.getTitle());
        assertEquals(TicketStatus.IN_PROGRESS, saved.getStatus());
    }

    @Test
    void patchTicketWithOnlyADescriptionLeavesEveryOtherFieldAloneAndWritesItThroughTheRepository() {
        // The description branch of patchTicket had no test at either layer: deleting
        // `if (description != null) existing.setDescription(description);` used to leave the
        // whole suite green.
        stubExisting(7L, new Ticket(7L, "Keyboard broken", "Space bar sticks", Priority.MEDIUM, TicketStatus.IN_PROGRESS));
        saveReturnsItsArgument();

        Ticket patched = ticketService.patchTicket(7L, null, "Space bar and enter key both stick", null, null);

        assertEquals("Space bar and enter key both stick", patched.getDescription());
        assertEquals("Keyboard broken", patched.getTitle());
        assertEquals(Priority.MEDIUM, patched.getPriority());
        assertEquals(TicketStatus.IN_PROGRESS, patched.getStatus());

        Ticket saved = captureSaved();
        assertEquals("Space bar and enter key both stick", saved.getDescription());
        assertEquals("Keyboard broken", saved.getTitle());
    }

    @Test
    void patchTicketWithOnlyAPriorityLeavesEveryOtherFieldAloneAndWritesItThroughTheRepository() {
        stubExisting(7L, new Ticket(7L, "Keyboard broken", "Space bar sticks", Priority.MEDIUM, TicketStatus.IN_PROGRESS));
        saveReturnsItsArgument();

        Ticket patched = ticketService.patchTicket(7L, null, null, Priority.HIGH, null);

        assertEquals(Priority.HIGH, patched.getPriority());
        assertEquals("Keyboard broken", patched.getTitle());
        assertEquals("Space bar sticks", patched.getDescription());
        assertEquals(TicketStatus.IN_PROGRESS, patched.getStatus());

        Ticket saved = captureSaved();
        assertEquals(Priority.HIGH, saved.getPriority());
        assertEquals(TicketStatus.IN_PROGRESS, saved.getStatus());
    }

    @Test
    void patchTicketWithAllNullsChangesNothingButStillWritesThroughTheRepository() {
        stubExisting(7L, new Ticket(7L, "Keyboard broken", "Space bar sticks", Priority.MEDIUM, TicketStatus.IN_PROGRESS));
        saveReturnsItsArgument();

        Ticket patched = ticketService.patchTicket(7L, null, null, null, null);

        assertEquals("Keyboard broken", patched.getTitle());
        assertEquals("Space bar sticks", patched.getDescription());
        assertEquals(Priority.MEDIUM, patched.getPriority());
        assertEquals(TicketStatus.IN_PROGRESS, patched.getStatus());

        // An all-null patch still takes the write path; it is a no-op in content, not in
        // behaviour. Pinned so the "changes nothing" reading is not mistaken for "skips save".
        Ticket saved = captureSaved();
        assertEquals("Keyboard broken", saved.getTitle());
        assertEquals(TicketStatus.IN_PROGRESS, saved.getStatus());
    }

    @Test
    void patchTicketThrowsNotFoundForAMissingId() {
        when(ticketRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> ticketService.patchTicket(404L, null, null, null, TicketStatus.RESOLVED));

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void deleteTicketRemovesTheTicketFromTheRepository() {
        stubExisting(7L, new Ticket(7L, "Keyboard broken", "Space bar sticks", Priority.MEDIUM, TicketStatus.NEW));

        ticketService.deleteTicket(7L);

        verify(ticketRepository).deleteById(7L);
    }

    @Test
    void deleteTicketOnAMissingIdThrowsNotFoundAndNeverDeletes() {
        when(ticketRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ticketService.deleteTicket(404L));

        // deleteById on a missing key would be a silent no-op, so only the interaction
        // distinguishes the deliberate fail-fast from doing nothing at all.
        verify(ticketRepository, never()).deleteById(anyLong());
    }

    private void stubExisting(long id, Ticket ticket) {
        when(ticketRepository.findById(id)).thenReturn(Optional.of(ticket));
    }

    private void saveReturnsItsArgument() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(call -> call.getArgument(0));
    }

    /**
     * Asserts that save() was called exactly once and returns what it was handed.
     *
     * The call itself is the load-bearing assertion: drop save() from the service and this
     * fails with "Wanted but not invoked". The field assertions callers then make on the
     * returned ticket are weaker than they look on the update/patch paths, because the
     * service mutates the stored instance in place and a captor holds a reference, not a
     * snapshot - they describe the entity's state now, which is its state at save time only
     * because nothing touches it afterwards. They still pin which fields the service set.
     */
    private Ticket captureSaved() {
        ArgumentCaptor<Ticket> saved = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(saved.capture());
        return saved.getValue();
    }
}
