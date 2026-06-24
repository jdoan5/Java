package com.johndoan.bookmarks;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer test that boots the full application context (controller +
 * service + in-memory repository) and drives it through MockMvc — no real
 * network needed. The DataSeeder runs, so two sample bookmarks already exist.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createReturnsCreatedAndEchoesFields() throws Exception {
        String json = """
                {
                  "title": "Example",
                  "url": "https://example.com",
                  "tags": ["Demo"],
                  "notes": "hello"
                }
                """;

        mockMvc.perform(post("/api/bookmarks")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Example"))
                // service lower-cases tags, so "Demo" -> "demo"
                .andExpect(jsonPath("$.tags[0]").value("demo"));
    }

    @Test
    void listReturnsSeededBookmarks() throws Exception {
        mockMvc.perform(get("/api/bookmarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
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
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingBookmarkReturns404() throws Exception {
        mockMvc.perform(get("/api/bookmarks/999999"))
                .andExpect(status().isNotFound());
    }
}
