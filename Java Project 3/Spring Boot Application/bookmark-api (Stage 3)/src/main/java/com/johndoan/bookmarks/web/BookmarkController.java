package com.johndoan.bookmarks.web;

import com.johndoan.bookmarks.domain.Bookmark;
import com.johndoan.bookmarks.service.BookmarkService;
import com.johndoan.bookmarks.web.dto.BookmarkResponse;
import com.johndoan.bookmarks.web.dto.CreateBookmarkRequest;
import com.johndoan.bookmarks.web.dto.UpdateBookmarkRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * REST endpoints for bookmarks.
 *
 *   GET    /api/bookmarks            list all (optional ?tag= filter)
 *   GET    /api/bookmarks/{id}       fetch one
 *   POST   /api/bookmarks            create one
 *   POST   /api/bookmarks/batch      create many (JSON array)
 *   PUT    /api/bookmarks/{id}       replace
 *   DELETE /api/bookmarks/{id}       delete
 */
@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkService service;
    private final Validator validator;

    public BookmarkController(BookmarkService service, Validator validator) {
        this.service = service;
        this.validator = validator;
    }

    @GetMapping
    public List<BookmarkResponse> list(@RequestParam(required = false) String tag) {
        return service.list(tag).stream()
                .map(BookmarkResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public BookmarkResponse get(@PathVariable Long id) {
        return BookmarkResponse.from(service.get(id));
    }

    @PostMapping
    public ResponseEntity<BookmarkResponse> create(@Valid @RequestBody CreateBookmarkRequest request,
                                                   UriComponentsBuilder uriBuilder) {
        Bookmark created = service.create(request);
        URI location = uriBuilder.path("/api/bookmarks/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(BookmarkResponse.from(created));
    }

    /**
     * Bulk create. Accepts a JSON ARRAY of bookmarks and creates them all in one
     * request — this is the endpoint to use instead of POSTing an array to the
     * single-create route (which expects one object and would 400).
     *
     * Important: {@code @Valid} does NOT cascade into a top-level {@code @RequestBody}
     * {@code List}, so we validate each element ourselves. Any violation throws
     * {@link jakarta.validation.ConstraintViolationException}, which the
     * {@link GlobalExceptionHandler} turns into a 400 — so one bad entry fails the
     * whole request and nothing is saved.
     */
    @PostMapping("/batch")
    public ResponseEntity<List<BookmarkResponse>> createBatch(@RequestBody List<CreateBookmarkRequest> requests) {
        for (CreateBookmarkRequest request : requests) {
            Set<ConstraintViolation<CreateBookmarkRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }
        List<BookmarkResponse> created = requests.stream()
                .map(service::create)
                .map(BookmarkResponse::from)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public BookmarkResponse update(@PathVariable Long id,
                                   @Valid @RequestBody UpdateBookmarkRequest request) {
        return BookmarkResponse.from(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
