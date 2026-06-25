package com.johndoan.bookmarks;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for the SECURED API (Stage 2).
 *
 * The {@code jwt(...)} post-processor injects an already-authenticated token
 * with chosen scopes, so we test authorization rules without standing up the
 * real /oauth2/token flow. A request with no jwt() is anonymous → 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createWithWriteScopeReturnsCreated() throws Exception {
        String json = """
                {
                  "title": "Example",
                  "url": "https://example.com",
                  "tags": ["Demo"],
                  "notes": "hello"
                }
                """;

        mockMvc.perform(post("/api/bookmarks")
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_bookmark.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Example"))
                // service lower-cases tags, so "Demo" -> "demo"
                .andExpect(jsonPath("$.tags[0]").value("demo"));
    }

    @Test
    void listWithReadScopeReturnsSeededBookmarks() throws Exception {
        mockMvc.perform(get("/api/bookmarks")
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_bookmark.read"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    void listWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/bookmarks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createWithOnlyReadScopeIsForbidden() throws Exception {
        String json = """
                {
                  "title": "Example",
                  "url": "https://example.com"
                }
                """;

        mockMvc.perform(post("/api/bookmarks")
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_bookmark.read")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
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
        String json = """
                {
                  "title": "",
                  "url": "https://example.com"
                }
                """;

        mockMvc.perform(post("/api/bookmarks")
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_bookmark.write")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingBookmarkReturns404() throws Exception {
        mockMvc.perform(get("/api/bookmarks/999999")
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_bookmark.read"))))
                .andExpect(status().isNotFound());
    }
}
