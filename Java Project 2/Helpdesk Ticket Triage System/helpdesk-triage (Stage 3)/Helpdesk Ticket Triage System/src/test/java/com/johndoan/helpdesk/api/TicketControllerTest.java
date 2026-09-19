package com.johndoan.helpdesk.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for the JPA-backed ticket API (Stage 3).
 *
 * <p>This class deliberately carries no {@code @Transactional}. A test-managed
 * transaction would hold every request of a test in one persistence context, so
 * a read-back after a PUT or PATCH would be answered from the first-level cache
 * and would report the new values even when the handler never wrote them. The
 * mutation "drop the {@code save(...)} call from the service" survived the whole
 * suite for exactly that reason. Without the annotation every request runs in
 * its own transaction and a read-back is a genuine round trip to H2.
 *
 * <p>The price is that rows survive from one test to the next in the shared
 * in-memory database, so nothing here may assert an absolute id, a row count or
 * a row order. Each test creates the ticket it needs and works with the id the
 * API hands back.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TicketControllerTest {

    private static final String PRINTER_JAM = """
            { "title": "Printer jam", "description": "Tray 2 keeps jamming", "priority": "LOW" }
            """;

    private static final String VPN_DOWN = """
            { "title": "VPN down", "description": "Cannot reach the gateway", "priority": "HIGH" }
            """;

    @Autowired
    private MockMvc mockMvc;

    /** Creates a ticket through the API and returns the generated id. */
    private long createTicket(String json) throws Exception {
        String body = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /**
     * Re-reads a ticket in a request of its own. This is the only way to tell a
     * write that happened from a handler echoing back the entity it just mutated
     * in memory, so every mutating test below ends with one of these.
     */
    private ResultActions readBack(long id) throws Exception {
        return mockMvc.perform(get("/api/tickets/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void createReturns201WithALocationHeaderAndTheSubmittedFields() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRINTER_JAM))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Printer jam"))
                .andExpect(jsonPath("$.description").value("Tray 2 keeps jamming"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andReturn();

        long id = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/tickets/" + id);
    }

    @Test
    void createDefaultsTheStatusToNewAndIgnoresAClientSuppliedOne() throws Exception {
        // A caller must not be able to file a ticket that skips the triage queue.
        String json = """
                { "title": "VPN down", "description": "Cannot reach the gateway",
                  "priority": "HIGH", "status": "CLOSED" }
                """;

        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn();

        long id = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
        readBack(id).andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void createRejectsABlankTitle() throws Exception {
        // Regression test for the missing @Valid on the POST handler: without it
        // this request returned 201 and persisted a ticket with an empty title.
        String json = """
                { "title": "", "description": "Tray 2 keeps jamming", "priority": "LOW" }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("title")));
    }

    @Test
    void createRejectsAMissingPriority() throws Exception {
        // Priority is non-null in the database, so without validation this failed
        // late and surfaced as a 500 rather than a client error.
        String json = """
                { "title": "VPN down", "description": "Cannot reach the gateway" }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("priority")));
    }

    @Test
    void createRejectsAPriorityThatIsNotOneOfTheEnumConstants() throws Exception {
        String json = """
                { "title": "VPN down", "description": "Cannot reach the gateway", "priority": "URGENT" }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("enum")));
    }

    @Test
    void getByIdReturnsTheTicketThatWasCreated() throws Exception {
        long id = createTicket(PRINTER_JAM);

        mockMvc.perform(get("/api/tickets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Printer jam"))
                .andExpect(jsonPath("$.description").value("Tray 2 keeps jamming"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void getAllListsTheTicketsThatExist() throws Exception {
        long printerJamId = createTicket(PRINTER_JAM);
        long vpnDownId = createTicket(VPN_DOWN);

        String body = mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // getAllTickets() calls findAll() with no Sort and the table is queried
        // without an ORDER BY, so row order is not part of the contract: select
        // each ticket by its own id instead of by position. The row count is not
        // assertable either, because the database is shared with the other tests.
        List<String> printerJamTitles = JsonPath.read(body, "$[?(@.id == " + printerJamId + ")].title");
        List<String> vpnDownTitles = JsonPath.read(body, "$[?(@.id == " + vpnDownId + ")].title");

        assertThat(printerJamTitles).containsExactly("Printer jam");
        assertThat(vpnDownTitles).containsExactly("VPN down");
    }

    @Test
    void getByIdForAnUnknownTicketReturns404WithAnApiErrorBody() throws Exception {
        mockMvc.perform(get("/api/tickets/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ticket not found: 999999"))
                .andExpect(jsonPath("$.path").value("/api/tickets/999999"));
    }

    @Test
    void patchWithOnlyAStatusSucceedsAndLeavesTheOtherFieldsIntact() throws Exception {
        // DO NOT add @Valid to the PATCH handler. UpdateTicketRequest is shared with
        // PUT and carries @NotBlank title plus @NotNull priority and status, so
        // validating it here would turn this exact request -- the one the Angular
        // front end sends to close a ticket -- into a 400. This test is the guard.
        long id = createTicket(PRINTER_JAM);

        mockMvc.perform(patch("/api/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"RESOLVED\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.title").value("Printer jam"))
                .andExpect(jsonPath("$.description").value("Tray 2 keeps jamming"))
                .andExpect(jsonPath("$.priority").value("LOW"));

        // The response above is serialised from the entity the handler just
        // mutated, so it would look identical if nothing had been written. Only
        // this second request can tell the two apart.
        readBack(id)
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.title").value("Printer jam"))
                .andExpect(jsonPath("$.description").value("Tray 2 keeps jamming"))
                .andExpect(jsonPath("$.priority").value("LOW"));
    }

    @Test
    void patchPersistsEveryFieldItIsGiven() throws Exception {
        long id = createTicket(PRINTER_JAM);

        String json = """
                { "title": "Printer replaced", "description": "Swapped for a new unit",
                  "priority": "HIGH", "status": "RESOLVED" }
                """;

        mockMvc.perform(patch("/api/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        readBack(id)
                .andExpect(jsonPath("$.title").value("Printer replaced"))
                .andExpect(jsonPath("$.description").value("Swapped for a new unit"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void patchOnAnUnknownTicketReturns404() throws Exception {
        mockMvc.perform(patch("/api/tickets/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"RESOLVED\" }"))
                .andExpect(status().isNotFound());
    }

    @Test
    void putReplacesEveryFieldOfTheTicket() throws Exception {
        long id = createTicket(PRINTER_JAM);

        String json = """
                { "title": "Printer replaced", "description": "Swapped for a new unit",
                  "priority": "HIGH", "status": "RESOLVED" }
                """;

        mockMvc.perform(put("/api/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Printer replaced"))
                .andExpect(jsonPath("$.description").value("Swapped for a new unit"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // Same reasoning as the PATCH test: the echo above proves nothing about
        // what reached the database.
        readBack(id)
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Printer replaced"))
                .andExpect(jsonPath("$.description").value("Swapped for a new unit"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void putRejectsABlankTitle() throws Exception {
        // The other half of the @Valid fix: an empty string satisfies the database
        // column, so before the fix this replaced a real title with nothing and
        // answered 200.
        long id = createTicket(PRINTER_JAM);

        String json = """
                { "title": "", "description": "Swapped for a new unit",
                  "priority": "HIGH", "status": "RESOLVED" }
                """;

        mockMvc.perform(put("/api/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("title")));

        // A rejected request must also leave the stored ticket alone.
        readBack(id)
                .andExpect(jsonPath("$.title").value("Printer jam"))
                .andExpect(jsonPath("$.description").value("Tray 2 keeps jamming"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void putRejectsAMissingPriority() throws Exception {
        // Each validated field of UpdateTicketRequest is pinned on its own.
        // Sending a valid priority in every PUT test, as putRejectsABlankTitle
        // does, would let @NotNull be deleted from priority with the suite green:
        // the failure would then only show up as a 500 from the not-null column.
        long id = createTicket(PRINTER_JAM);

        String json = """
                { "title": "Printer replaced", "description": "Swapped for a new unit",
                  "status": "RESOLVED" }
                """;

        mockMvc.perform(put("/api/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("priority")));

        readBack(id).andExpect(jsonPath("$.priority").value("LOW"));
    }

    @Test
    void putRejectsAMissingStatus() throws Exception {
        // The companion of putRejectsAMissingPriority for the other @NotNull.
        long id = createTicket(PRINTER_JAM);

        String json = """
                { "title": "Printer replaced", "description": "Swapped for a new unit",
                  "priority": "HIGH" }
                """;

        mockMvc.perform(put("/api/tickets/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("status")));

        readBack(id).andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void putOnAnUnknownTicketReturns404() throws Exception {
        String json = """
                { "title": "Printer replaced", "description": "Swapped for a new unit",
                  "priority": "HIGH", "status": "RESOLVED" }
                """;

        mockMvc.perform(put("/api/tickets/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204AndTheTicketIsGoneAfterwards() throws Exception {
        long id = createTicket(PRINTER_JAM);

        mockMvc.perform(delete("/api/tickets/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tickets/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteOnAnUnknownTicketReturns404() throws Exception {
        mockMvc.perform(delete("/api/tickets/999999"))
                .andExpect(status().isNotFound());
    }
}
