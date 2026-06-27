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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
 * REST endpoints for bookmarks. Every method derives the current owner from the
 * JWT subject ({@code @AuthenticationPrincipal Jwt}) and passes it to the
 * service, so the data each caller sees is automatically scoped to them.
 *
 *   GET    /api/bookmarks            list MY bookmarks (optional ?tag= filter)
 *   GET    /api/bookmarks/{id}       fetch one of MINE
 *   POST   /api/bookmarks            create one (owned by me)
 *   POST   /api/bookmarks/batch      create many (JSON array, owned by me)
 *   PUT    /api/bookmarks/{id}       replace one of MINE
 *   DELETE /api/bookmarks/{id}       delete one of MINE
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
    public List<BookmarkResponse> list(@RequestParam(required = false) String tag,
                                       @AuthenticationPrincipal Jwt jwt) {
        return service.list(jwt.getSubject(), tag).stream()
                .map(BookmarkResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public BookmarkResponse get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return BookmarkResponse.from(service.get(id, jwt.getSubject()));
    }

    @PostMapping
    public ResponseEntity<BookmarkResponse> create(@Valid @RequestBody CreateBookmarkRequest request,
                                                   @AuthenticationPrincipal Jwt jwt,
                                                   UriComponentsBuilder uriBuilder) {
        Bookmark created = service.create(request, jwt.getSubject());
        URI location = uriBuilder.path("/api/bookmarks/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(BookmarkResponse.from(created));
    }

    /**
     * Bulk create. Accepts a JSON ARRAY of bookmarks (all owned by the caller).
     *
     * {@code @Valid} does NOT cascade into a top-level {@code @RequestBody List},
     * so we validate each element ourselves; a violation throws
     * {@link jakarta.validation.ConstraintViolationException} -> 400, and nothing
     * is saved.
     */
    @PostMapping("/batch")
    public ResponseEntity<List<BookmarkResponse>> createBatch(@RequestBody List<CreateBookmarkRequest> requests,
                                                              @AuthenticationPrincipal Jwt jwt) {
        for (CreateBookmarkRequest request : requests) {
            Set<ConstraintViolation<CreateBookmarkRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        }
        List<BookmarkResponse> created = requests.stream()
                .map(request -> service.create(request, jwt.getSubject()))
                .map(BookmarkResponse::from)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public BookmarkResponse update(@PathVariable Long id,
                                   @Valid @RequestBody UpdateBookmarkRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return BookmarkResponse.from(service.update(id, request, jwt.getSubject()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.delete(id, jwt.getSubject());
    }
}
