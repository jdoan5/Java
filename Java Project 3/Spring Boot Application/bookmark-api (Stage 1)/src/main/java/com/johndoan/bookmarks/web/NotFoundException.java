package com.johndoan.bookmarks.web;

/**
 * Thrown when a requested bookmark does not exist. Translated into an
 * HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
