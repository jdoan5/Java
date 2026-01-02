package com.johndoan.jobtracker;

import java.time.LocalDate;

public final class Validation {

    private Validation() { }

    public static void requireNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
    }

    public static void requireDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Date range invalid: From date must be <= To date.");
        }
    }
}
