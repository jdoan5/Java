package com.johndoan.bookmarks;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for the SECURED, JPA-backed, per-user API (Stage 4).
 *
 * The {@code jwt()} post-processor injects an authenticated token; here we also
 * set its {@code subject}, because the controller uses the subject as the
 * bookmark owner. Authorities are set explicitly to the scopes under test.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookmarkControllerTest {

    private static final SimpleGrantedAuthority READ = new SimpleGrantedAuthority("SCOPE_bookmark.read");
    private static final SimpleGrantedAuthority WRITE = new SimpleGrantedAuthority("SCOPE_bookmark.write");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createIsOwnedByTheJwtSubject() throws Exception {
        String json = """
                { "title": "Alice link", "url": "https://example.com/alice" }
                """;

        mockMvc.perform(post("/api/bookmarks")
                        .with(jwt().jwt(b -> b.subject("alice")).authorities(WRITE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner").value("alice"))
                .andExpect(jsonPath("$.title").value("Alice link"));
    }

    @Test
    void usersOnlySeeTheirOwnBookmarks() throws Exception {
        // Alice creates a bookmark.
        mockMvc.perform(post("/api/bookmarks")
                        .with(jwt().jwt(b -> b.subject("alice")).authorities(WRITE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"Alice only\", \"url\": \"https://example.com/a\" }"))
                .andExpect(status().isCreated());

        // Bob (a different subject, no seeded data) sees none of Alice's.
        mockMvc.perform(get("/api/bookmarks")
                        .with(jwt().jwt(b -> b.subject("bob")).authorities(READ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void cannotFetchAnotherUsersBookmark() throws Exception {
        // id 1 is seeded for "bruno-client"; "bob" does not own it, so the
        // owner-scoped lookup returns 404 rather than leaking someone else's data.
        mockMvc.perform(get("/api/bookmarks/1")
                        .with(jwt().jwt(b -> b.subject("bob")).authorities(READ)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/bookmarks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithOnlyReadScopeIsForbidden() throws Exception {
        mockMvc.perform(post("/api/bookmarks")
                        .with(jwt().jwt(b -> b.subject("alice")).authorities(READ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"x\", \"url\": \"https://example.com\" }"))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicHealthIsOpenWithoutToken() throws Exception {
        mockMvc.perform(get("/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void validationRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/bookmarks")
                        .with(jwt().jwt(b -> b.subject("alice")).authorities(WRITE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"title\": \"\", \"url\": \"https://example.com\" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingBookmarkReturns404() throws Exception {
        mockMvc.perform(get("/api/bookmarks/999999")
                        .with(jwt().jwt(b -> b.subject("alice")).authorities(READ)))
                .andExpect(status().isNotFound());
    }

    @Test
    void batchCreateWithWriteScopeCreatesAll() throws Exception {
        String json = """
                [
                  { "title": "First",  "url": "https://example.com/1" },
                  { "title": "Second", "url": "https://example.com/2", "tags": ["x"] }
                ]
                """;

        mockMvc.perform(post("/api/bookmarks/batch")
                        .with(jwt().jwt(b -> b.subject("alice")).authorities(WRITE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].owner").value("alice"))
                .andExpect(jsonPath("$[1].title").value("Second"));
    }

    @Test
    void batchCreateRejectsAnInvalidElement() throws Exception {
        String json = """
                [
                  { "title": "Good", "url": "https://example.com/ok" },
                  { "title": "",     "url": "not-a-url" }
                ]
                """;

        mockMvc.perform(post("/api/bookmarks/batch")
                        .with(jwt().jwt(b -> b.subject("alice")).authorities(WRITE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void batchCreateWithoutWriteScopeIsForbidden() throws Exception {
        mockMvc.perform(post("/api/bookmarks/batch")
                        .with(jwt().jwt(b -> b.subject("alice")).authorities(READ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[ { \"title\": \"Nope\", \"url\": \"https://example.com/3\" } ]"))
                .andExpect(status().isForbidden());
    }
}
