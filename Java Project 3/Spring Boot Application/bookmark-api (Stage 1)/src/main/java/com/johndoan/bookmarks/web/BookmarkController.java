package com.johndoan.bookmarks.web;

import com.johndoan.bookmarks.domain.Bookmark;
import com.johndoan.bookmarks.service.BookmarkService;
import com.johndoan.bookmarks.web.dto.BookmarkResponse;
import com.johndoan.bookmarks.web.dto.CreateBookmarkRequest;
import com.johndoan.bookmarks.web.dto.UpdateBookmarkRequest;
import jakarta.validation.Valid;
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

/**
 * REST endpoints for bookmarks.
 *
 *   GET    /api/bookmarks            list all (optional ?tag= filter)
 *   GET    /api/bookmarks/{id}       fetch one
 *   POST   /api/bookmarks            create
 *   PUT    /api/bookmarks/{id}       replace
 *   DELETE /api/bookmarks/{id}       delete
 */
@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkService service;

    public BookmarkController(BookmarkService service) {
        this.service = service;
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
