package com.johndoan.bookmarks.config;

import com.johndoan.bookmarks.repository.BookmarkRepository;
import com.johndoan.bookmarks.service.BookmarkService;
import com.johndoan.bookmarks.web.dto.CreateBookmarkRequest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds a couple of sample bookmarks ONLY when the database is empty.
 *
 * Now that bookmarks are persisted to a file, we must not re-seed on every
 * startup (that would pile up duplicates). The {@code count() == 0} guard makes
 * this run exactly once, on a fresh database.
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
            return; // database already has data — leave it alone
        }

        service.create(new CreateBookmarkRequest(
                "Spring Boot Reference",
                "https://docs.spring.io/spring-boot/index.html",
                List.of("spring", "reference"),
                "Official Spring Boot documentation."
        ));
        service.create(new CreateBookmarkRequest(
                "Bruno API Client",
                "https://www.usebruno.com",
                List.of("tools", "api"),
                "Open-source, git-friendly API client."
        ));
    }
}
