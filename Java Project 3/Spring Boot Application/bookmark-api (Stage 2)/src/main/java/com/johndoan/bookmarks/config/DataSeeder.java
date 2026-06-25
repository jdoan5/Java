package com.johndoan.bookmarks.config;

import com.johndoan.bookmarks.service.BookmarkService;
import com.johndoan.bookmarks.web.dto.CreateBookmarkRequest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds a couple of sample bookmarks on startup so the very first
 * GET /api/bookmarks in Bruno returns something interesting.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final BookmarkService service;

    public DataSeeder(BookmarkService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
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
