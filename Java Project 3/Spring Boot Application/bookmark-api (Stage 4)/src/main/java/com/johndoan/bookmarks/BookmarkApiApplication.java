package com.johndoan.bookmarks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point.
 *
 * {@code @SpringBootApplication} bundles three annotations:
 *   - @Configuration       (this class can declare beans)
 *   - @EnableAutoConfiguration (Spring Boot wires up Tomcat, Jackson, etc.)
 *   - @ComponentScan       (find @RestController/@Service/@Repository in this package)
 */
@SpringBootApplication
public class BookmarkApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookmarkApiApplication.class, args);
    }
}
