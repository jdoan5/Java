package com.johndoan.helpdesk.api;

import com.jayway.jsonpath.JsonPath;
import com.johndoan.helpdesk.domain.Ticket;
import com.johndoan.helpdesk.repo.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for the in-memory ticket API (Stage 2).
 *
 * Two things about this stage shape the tests. The repository is a singleton bean whose
 * map is never cleared and the service's id generator is a singleton too, so every method
 * clears the store in {@code @BeforeEach}, creates the ticket it needs inside the test
 * body, and reads that ticket's id out of the create response instead of assuming the
 * sequence restarts at 1. There is deliberately no class-level {@code @Transactional}:
 * every read-back below goes back through the dispatcher servlet as its own request.
 *
 * WHAT THESE TESTS DO NOT PROVE. Every PUT and PATCH here is followed by a separate GET, so
 * the values are asserted against a fresh read rather than against the mutating call's own
 * echo of the entity it just changed. On this stage that read still is not a persistence
 * proof. InMemoryTicketRepository stores the Ticket instance itself; TicketService loads
 * that instance, mutates it setter by setter, and hands the very same object to save(). The
 * stored object and the mutated object are one object, so deleting the save() call outright
 * would leave every assertion in this class green - no HTTP-level assertion can tell the
 * two apart while that aliasing holds. The contract "an update is written through the
 * repository" is therefore pinned in TicketServiceTest, with an explicit
 * verify(ticketRepository).save(...) against a mock, which is the only place on this stage
 * where the write itself is observable. What the read-backs here do buy is everything
 * downstream of the write: that a later GET resolves the id, re-maps the entity, and
 * reports the new values rather than a stale or cached copy.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void clearTickets() {
        // The store survives between test methods; without this, list assertions would
        // depend on execution order.
        for (Ticket ticket : ticketRepository.findAll()) {
            ticketRepository.deleteById(ticket.getId());
        }
    }

    @Test
    void createReturns201WithLocationHeaderAndDefaultsToNew() throws Exception {
        String json = """
                { "title": "Printer jam", "description": "Tray 2 keeps jamming", "priority": "HIGH" }
                """;

        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Printer jam"))
                .andExpect(jsonPath("$.description").value("Tray 2 keeps jamming"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                // The service, not the request, picks the opening status. A body that tries
                // to choose one is covered by createIgnoresAStatusSentByTheClient.
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn();

        long id = idOf(result);
        assertEquals("/api/tickets/" + id, result.getResponse().getHeader("Location"));
    }

    @Test
    void createIgnoresAStatusSentByTheClient() throws Exception {
        // CreateTicketRequest has no status property, and Spring Boot leaves Jackson's
        // FAIL_ON_UNKNOWN_PROPERTIES off, so a status in the create body is dropped in
        // silence rather than rejected. Asserted rather than assumed: this is the test that
        // earns the "the client does not get to choose the opening status" claim above.
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Opens closed?", "description": "d", "priority": "LOW", "status": "CLOSED" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn();

        assertTicketReads(idOf(result), "Opens closed?", "d", "LOW", "NEW");
    }

    @Test
    void createTrimsSurroundingWhitespace() throws Exception {
        String json = """
                { "title": "   Padded   ", "description": "   spaced out   ", "priority": "LOW" }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Padded"))
                .andExpect(jsonPath("$.description").value("spaced out"));
    }

    @Test
    void createWithBlankTitleReturns400() throws Exception {
        // Regression test for the @Valid fix. Without @Valid on the create handler the blank
        // title slips past bean validation, trips the domain guard instead, and the catch-all
        // exception handler flattens it into a 500 with the field name lost.
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"\", \"description\": \"y\", \"priority\": \"LOW\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("title")));
    }

    @Test
    void createWithMissingPriorityReturns400() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"No priority\", \"description\": \"y\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("priority")));
    }

    @Test
    void createWithBlankTitlePersistsNothing() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"\", \"description\": \"y\", \"priority\": \"LOW\" }"))
                .andExpect(status().isBadRequest());

        // A count is safe here only because @BeforeEach empties the store; the assertion is
        // "the rejected create added nothing", not "the API holds exactly n tickets".
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void createWithUnknownPriorityReturns400() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"Bad enum\", \"description\": \"y\", \"priority\": \"URGENT\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON or invalid enum value"));
    }

    @Test
    void createWithMalformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"Truncated\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON or invalid enum value"));
    }

    @Test
    void getByIdReturnsTheStoredTicket() throws Exception {
        long id = createTicket("Laptop won't boot", "Black screen on power-up", "MEDIUM");

        mockMvc.perform(get("/api/tickets/" + id))
                .andExpect(status().isOk())
                // JSON numbers come back as Integer, so compare in that type.
                .andExpect(jsonPath("$.id").value(Math.toIntExact(id)))
                .andExpect(jsonPath("$.title").value("Laptop won't boot"))
                .andExpect(jsonPath("$.description").value("Black screen on power-up"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void listReturnsEveryCreatedTicket() throws Exception {
        createTicket("First ticket", "d", "LOW");
        createTicket("Second ticket", "d", "HIGH");

        // The store is a map, so the order it hands back is not part of the contract. The
        // count is meaningful only because @BeforeEach emptied the store first.
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("First ticket", "Second ticket")));
    }

    @Test
    void missingTicketReturns404WithApiErrorBody() throws Exception {
        mockMvc.perform(get("/api/tickets/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Ticket not found: 999999"))
                .andExpect(jsonPath("$.path").value("/api/tickets/999999"));
    }

    @Test
    void replaceUpdatesEveryFieldAndASeparateGetSeesIt() throws Exception {
        long id = createTicket("Original", "Original description", "LOW");

        String json = """
                {
                  "title": "Replaced",
                  "description": "Replaced description",
                  "priority": "HIGH",
                  "status": "CLOSED"
                }
                """;

        mockMvc.perform(put("/api/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Replaced"))
                .andExpect(jsonPath("$.status").value("CLOSED"));

        assertTicketReads(id, "Replaced", "Replaced description", "HIGH", "CLOSED");
    }

    @Test
    void replaceWithBlankTitleReturns400() throws Exception {
        long id = createTicket("Original", "Original description", "LOW");

        String json = """
                { "title": "", "description": "d", "priority": "HIGH", "status": "CLOSED" }
                """;

        mockMvc.perform(put("/api/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("title")));

        // The rejected PUT must not have moved the ticket half-way.
        assertTicketReads(id, "Original", "Original description", "LOW", "NEW");
    }

    @Test
    void replaceWithoutStatusReturns400() throws Exception {
        long id = createTicket("Original", "Original description", "LOW");

        // PUT replaces the whole resource, so an incomplete body is rejected. This is the
        // one behaviour the @Valid fix changes for a previously-succeeding request, which is
        // why it is asserted explicitly rather than left implicit.
        mockMvc.perform(put("/api/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"Replaced\", \"description\": \"d\", \"priority\": \"HIGH\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("status")));

        assertTicketReads(id, "Original", "Original description", "LOW", "NEW");
    }

    @Test
    void replaceMissingTicketReturns404() throws Exception {
        String json = """
                { "title": "Replaced", "description": "d", "priority": "HIGH", "status": "CLOSED" }
                """;

        mockMvc.perform(put("/api/tickets/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchWithOnlyStatusSucceeds() throws Exception {
        long id = createTicket("Keyboard broken", "Space bar sticks", "MEDIUM");

        // Stage 2's own Bruno collection exercises this endpoint, and patchTicket exists to
        // apply the non-null fields only. UpdateTicketRequest marks title @NotBlank and both
        // priority and status @NotNull, so adding @Valid to the PATCH handler would turn a
        // status-only body into a 400 and leave no way to move a ticket through its
        // lifecycle without resending the whole resource - which is what PUT is already for.
        // This test is the guard against that.
        mockMvc.perform(patch("/api/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"RESOLVED\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.title").value("Keyboard broken"))
                .andExpect(jsonPath("$.description").value("Space bar sticks"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"));

        assertTicketReads(id, "Keyboard broken", "Space bar sticks", "MEDIUM", "RESOLVED");
    }

    @Test
    void patchWithOnlyTitleLeavesEveryOtherFieldAlone() throws Exception {
        long id = createTicket("Keyboard broken", "Space bar sticks", "MEDIUM");

        mockMvc.perform(patch("/api/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"Keyboard replaced\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Keyboard replaced"));

        assertTicketReads(id, "Keyboard replaced", "Space bar sticks", "MEDIUM", "NEW");
    }

    @Test
    void patchWithOnlyDescriptionLeavesEveryOtherFieldAlone() throws Exception {
        long id = createTicket("Keyboard broken", "Space bar sticks", "MEDIUM");

        // description is the one field patchTicket handles that nothing else pins: without
        // this test, deleting its `if (description != null)` branch changes no result.
        mockMvc.perform(patch("/api/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"description\": \"Space bar and enter key both stick\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Space bar and enter key both stick"));

        assertTicketReads(id, "Keyboard broken", "Space bar and enter key both stick", "MEDIUM", "NEW");
    }

    @Test
    void patchWithOnlyPriorityLeavesStatusAlone() throws Exception {
        long id = createTicket("Keyboard broken", "Space bar sticks", "MEDIUM");

        mockMvc.perform(patch("/api/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"IN_PROGRESS\" }"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"priority\": \"HIGH\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        assertTicketReads(id, "Keyboard broken", "Space bar sticks", "HIGH", "IN_PROGRESS");
    }

    @Test
    void patchWithAnEmptyJsonObjectIsANoOp() throws Exception {
        long id = createTicket("Keyboard broken", "Space bar sticks", "MEDIUM");

        // "{}" is an empty OBJECT, not an empty body: it binds fine and every field of
        // UpdateTicketRequest arrives null, so patchTicket skips all four branches. The
        // genuinely empty body is a different case - see patchWithATrulyEmptyBodyReturns400.
        mockMvc.perform(patch("/api/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Keyboard broken"))
                .andExpect(jsonPath("$.description").value("Space bar sticks"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.status").value("NEW"));

        assertTicketReads(id, "Keyboard broken", "Space bar sticks", "MEDIUM", "NEW");
    }

    @Test
    void patchWithATrulyEmptyBodyReturns400() throws Exception {
        long id = createTicket("Keyboard broken", "Space bar sticks", "MEDIUM");

        // The stage's Bruno request "PATCH Partial Update.bru" is saved with `body: none`.
        // Sending it as saved does not reach the handler at all: there is nothing for the
        // message converter to bind, so it fails as unreadable and the global handler turns
        // it into the same 400 a malformed body gets. Documented here because the Bruno
        // collection is the stage's manual test plan, and running it as checked in produces
        // this response, not the no-op above.
        mockMvc.perform(patch("/api/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON or invalid enum value"));

        assertTicketReads(id, "Keyboard broken", "Space bar sticks", "MEDIUM", "NEW");
    }

    @Test
    void patchMissingTicketReturns404() throws Exception {
        mockMvc.perform(patch("/api/tickets/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"RESOLVED\" }"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204AndTheTicketIsGone() throws Exception {
        long id = createTicket("Mouse missing", "Not at the desk", "LOW");

        mockMvc.perform(delete("/api/tickets/" + id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/tickets/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteMissingTicketReturns404() throws Exception {
        // The service looks the ticket up first, so a missing id fails loudly instead of
        // reporting a successful no-op delete.
        mockMvc.perform(delete("/api/tickets/999999"))
                .andExpect(status().isNotFound());
    }

    /** Creates a ticket over HTTP and returns the id the API assigned it. */
    private long createTicket(String title, String description, String priority) throws Exception {
        String json = """
                { "title": "%s", "description": "%s", "priority": "%s" }
                """.formatted(title, description, priority);

        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        return idOf(result);
    }

    /**
     * Re-reads the ticket in a separate GET request and asserts every field, so the values
     * asserted are the ones a later reader sees rather than the mutating call's own echo.
     * Read the class comment before treating a green result here as proof of a write.
     */
    private void assertTicketReads(long id, String title, String description, String priority, String status)
            throws Exception {
        mockMvc.perform(get("/api/tickets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.description").value(description))
                .andExpect(jsonPath("$.priority").value(priority))
                .andExpect(jsonPath("$.status").value(status));
    }

    private long idOf(MvcResult result) throws Exception {
        Number id = JsonPath.parse(result.getResponse().getContentAsString()).read("$.id");
        return id.longValue();
    }
}
