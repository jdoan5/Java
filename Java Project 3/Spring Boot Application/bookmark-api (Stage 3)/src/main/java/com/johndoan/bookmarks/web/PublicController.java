package com.johndoan.bookmarks.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * An unauthenticated endpoint. In Stage 1 every endpoint is open, but having
 * a dedicated public route makes Stage 2 (OAuth) easy to demonstrate: this
 * stays open while /api/** becomes protected.
 */
@RestController
@RequestMapping("/public")
public class PublicController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "bookmark-api",
                "time", Instant.now().toString()
        );
    }
}
