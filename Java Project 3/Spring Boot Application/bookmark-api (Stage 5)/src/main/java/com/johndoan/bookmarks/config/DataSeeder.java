package com.johndoan.bookmarks.config;

import com.johndoan.bookmarks.repository.BookmarkRepository;
import com.johndoan.bookmarks.service.BookmarkService;
import com.johndoan.bookmarks.web.dto.CreateBookmarkRequest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds sample bookmarks for TWO different owners (only when the database is
 * empty). This makes per-user ownership visible immediately:
 *
 *   - "bruno-client" — what the client_credentials app token sees.
 *   - "john"         — what the user sees after logging in via authorization_code.
 *
 * Each only ever sees their own two bookmarks.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final BookmarkService service;
    private final BookmarkRepository repository;

    public DataSeeder(BookmarkService service, BookmarkRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return; // already seeded
        }

        service.create(new CreateBookmarkRequest(
                "Spring Boot Reference",
                "https://docs.spring.io/spring-boot/index.html",
                List.of("spring", "reference"),
                "Seeded for the app (client_credentials) token."
        ), "bruno-client");
        service.create(new CreateBookmarkRequest(
                "Bruno API Client",
                "https://www.usebruno.com",
                List.of("tools", "api"),
                "Seeded for the app (client_credentials) token."
        ), "bruno-client");

        service.create(new CreateBookmarkRequest(
                "Spring Security Reference",
                "https://docs.spring.io/spring-security/reference/",
                List.of("spring", "security"),
                "Seeded for user 'john'."
        ), "john");
        service.create(new CreateBookmarkRequest(
                "OAuth 2.0 Simplified",
                "https://www.oauth.com",
                List.of("oauth", "reference"),
                "Seeded for user 'john'."
        ), "john");
    }
}
