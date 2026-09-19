package com.johndoan.helpdesk.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.LinkedHashMap;
import java.util.Map;

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
 * HTTP-layer tests for the ticket API: status codes, JSON shape, the 404 path and
 * the validation contract.
 *
 * <p>PERSISTENCE. A PUT or PATCH response body is serialised from the very entity the
 * handler just mutated in memory, so it reports the new values whether or not anything
 * was written. Asserting only that echo would leave a service that dropped its
 * {@code save(...)} call completely undetected. Every mutating test here therefore
 * follows up with a SEPARATE GET and asserts the new values on THAT response.
 *
 * <p>For the read-back to mean anything the class must NOT be {@code @Transactional}:
 * a rolled-back test transaction would share one persistence context with the request,
 * so the GET could be answered from memory and a missing write would be invisible.
 * Isolation comes instead from every test creating its own ticket and using the id it
 * gets back — no test asserts an absolute id or an absolute list length.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TicketControllerTest {

    private static final long MISSING_ID = 999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Builds a JSON body from field/value pairs. Jackson does the escaping, so a title
     * containing a quote or a backslash still produces well-formed JSON — string
     * interpolation into a JSON template does not. A null value is emitted as JSON null;
     * to omit a field, leave it out of the call.
     */
    private String json(Object... fieldsAndValues) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int i = 0; i < fieldsAndValues.length; i += 2) {
            body.put((String) fieldsAndValues[i], fieldsAndValues[i + 1]);
        }
        return objectMapper.writeValueAsString(body);
    }

    /** Creates a ticket over HTTP and hands back its generated id. */
    private long createTicket(String title, String description, String priority) throws Exception {
        String body = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", title, "description", description, "priority", priority)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body).get("id").asLong();
    }

    /**
     * Re-reads the ticket in a fresh request, so the assertions that follow run against
     * what was actually stored rather than against the mutating call's own echo.
     */
    private ResultActions readBack(long id) throws Exception {
        return mockMvc.perform(get("/api/tickets/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    void createReturns201WithALocationHeaderAndAStatusOfNew() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(
                                "title", "VPN drops every hour",
                                "description", "Since the 3.5 upgrade",
                                "priority", "HIGH")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("VPN drops every hour"))
                .andExpect(jsonPath("$.description").value("Since the 3.5 upgrade"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn();

        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        // The Location header must point at the ticket that was actually created.
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/tickets/" + id);
    }

    @Test
    void getByIdRoundTripsTheCreatedTicket() throws Exception {
        long id = createTicket("Monitor flickers", "Second screen only", "LOW");

        readBack(id)
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Monitor flickers"))
                .andExpect(jsonPath("$.description").value("Second screen only"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void aTitleContainingQuotesAndBackslashesSurvivesTheRoundTrip() throws Exception {
        // Guards the JSON body helper: built with Jackson rather than string interpolation,
        // so these characters are escaped instead of producing a malformed request.
        String awkward = "Printer says \"PC LOAD LETTER\" on C:\\spool";

        long id = createTicket(awkward, "Reported by \"Bob\"", "LOW");

        readBack(id)
                .andExpect(jsonPath("$.title").value(awkward))
                .andExpect(jsonPath("$.description").value("Reported by \"Bob\""));
    }

    @Test
    void missingTicketReturns404WithTheApiErrorBody() throws Exception {
        mockMvc.perform(get("/api/tickets/{id}", MISSING_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString(String.valueOf(MISSING_ID))))
                .andExpect(jsonPath("$.path").value("/api/tickets/" + MISSING_ID));
    }

    @Test
    void listIncludesANewlyCreatedTicket() throws Exception {
        long id = createTicket("Badge reader offline", "North entrance", "MEDIUM");

        // Filtered by the captured id, so the assertion survives whatever else the suite created.
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].title").value("Badge reader offline"))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].status").value("NEW"));
    }

    @Test
    void putReplacesEveryFieldIncludingStatus() throws Exception {
        long id = createTicket("Mailbox full", "Cannot receive mail", "MEDIUM");

        mockMvc.perform(put("/api/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(
                                "title", "Mailbox quota raised",
                                "description", "Bumped to 50GB",
                                "priority", "LOW",
                                "status", "RESOLVED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Mailbox quota raised"))
                .andExpect(jsonPath("$.description").value("Bumped to 50GB"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // The assertions that matter: a PUT that mutated the entity but never persisted it
        // would satisfy every expectation above and fail every one below.
        readBack(id)
                .andExpect(jsonPath("$.title").value("Mailbox quota raised"))
                .andExpect(jsonPath("$.description").value("Bumped to 50GB"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void putForAnUnknownIdReturns404() throws Exception {
        // A complete body, so this reaches the service and 404s rather than failing validation.
        mockMvc.perform(put("/api/tickets/{id}", MISSING_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(
                                "title", "Ghost ticket",
                                "description", "Does not exist",
                                "priority", "LOW",
                                "status", "NEW")))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchWithOnlyAStatusStillSucceeds() throws Exception {
        long id = createTicket("Password reset", "Locked out after 5 tries", "HIGH");

        // GUARD: PATCH is a partial update. TicketService.updatePartial in the Angular client
        // sends exactly this shape, and frontend/helpdesk-ui's ticket.service.spec.ts pins it.
        // (No screen calls it yet — the contract lives in the service and its spec.)
        // If anyone adds @Valid to the PATCH handler, UpdateTicketRequest's @NotBlank title and
        // @NotNull priority/status would turn this into a 400. This test fails the moment that
        // happens.
        mockMvc.perform(patch("/api/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("status", "RESOLVED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.title").value("Password reset"))
                .andExpect(jsonPath("$.description").value("Locked out after 5 tries"))
                .andExpect(jsonPath("$.priority").value("HIGH"));

        // Read back: the new status must have been written, and the untouched fields kept.
        readBack(id)
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.title").value("Password reset"))
                .andExpect(jsonPath("$.description").value("Locked out after 5 tries"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void patchWithOnlyAPriorityStillSucceeds() throws Exception {
        long id = createTicket("Slow build agent", "CI queue backing up", "LOW");

        mockMvc.perform(patch("/api/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("priority", "HIGH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.title").value("Slow build agent"))
                .andExpect(jsonPath("$.description").value("CI queue backing up"))
                .andExpect(jsonPath("$.status").value("NEW"));

        readBack(id)
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.title").value("Slow build agent"))
                .andExpect(jsonPath("$.description").value("CI queue backing up"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void patchWithOnlyATitleStillSucceeds() throws Exception {
        long id = createTicket("Typo in the ticket title", "Raised by the service desk", "LOW");

        mockMvc.perform(patch("/api/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", "Corrected title")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Corrected title"));

        readBack(id)
                .andExpect(jsonPath("$.title").value("Corrected title"))
                .andExpect(jsonPath("$.description").value("Raised by the service desk"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void patchWithOnlyADescriptionStillSucceeds() throws Exception {
        long id = createTicket("Scanner jams", "First draft", "MEDIUM");

        mockMvc.perform(patch("/api/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("description", "Jams on duplex only, tray 2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Jams on duplex only, tray 2"));

        readBack(id)
                .andExpect(jsonPath("$.description").value("Jams on duplex only, tray 2"))
                .andExpect(jsonPath("$.title").value("Scanner jams"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void patchForAnUnknownIdReturns404() throws Exception {
        mockMvc.perform(patch("/api/tickets/{id}", MISSING_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("status", "CLOSED")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204AndTheTicketIsThenGone() throws Exception {
        long id = createTicket("Spare keyboard", "For the loan pool", "LOW");

        mockMvc.perform(delete("/api/tickets/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tickets/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteForAnUnknownIdReturns404() throws Exception {
        mockMvc.perform(delete("/api/tickets/{id}", MISSING_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void createWithAnUnrecognisedPriorityIsRejected() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", "Urgent thing", "description", "x", "priority", "URGENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON or invalid enum value"));
    }

    @Test
    void createWithMalformedJsonIsRejected() throws Exception {
        // Deliberately hand-written: the point is a body Jackson cannot parse.
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"broken\", "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithABlankTitleIsRejected() throws Exception {
        // REGRESSION GUARD for the @Valid fix on POST. Without @Valid this returned 201 and
        // persisted a ticket with an empty title; the DTO's @NotBlank was never evaluated.
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", "", "description", "y", "priority", "LOW")))
                .andExpect(status().isBadRequest())
                // Match on the field name only: the default constraint sentence is locale-dependent.
                .andExpect(jsonPath("$.message").value(containsString("title")));
    }

    @Test
    void createWithoutAPriorityIsRejected() throws Exception {
        // REGRESSION GUARD for the @Valid fix on POST. Without @Valid this fell through to the
        // H2 NOT NULL constraint and surfaced as a 500 "Unexpected server error".
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", "No priority given", "description", "y")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("priority")));
    }

    @Test
    void putWithAPartialBodyIsRejected() throws Exception {
        long id = createTicket("Needs a full replace", "PUT is all-or-nothing", "MEDIUM");

        // REGRESSION GUARD for the @Valid fix on PUT, and the counterpart to
        // patchWithOnlyAStatusStillSucceeds: PUT replaces the whole ticket, so a partial body
        // is a client error (it used to 500 on the NOT NULL constraint), while the same body
        // is perfectly legal on PATCH.
        mockMvc.perform(put("/api/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("status", "RESOLVED")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("title")));

        // Rejected means nothing was written: the ticket is untouched.
        readBack(id)
                .andExpect(jsonPath("$.title").value("Needs a full replace"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }
}
