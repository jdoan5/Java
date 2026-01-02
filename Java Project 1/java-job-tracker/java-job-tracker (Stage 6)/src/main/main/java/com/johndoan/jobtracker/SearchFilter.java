package com.johndoan.jobtracker;

import java.time.LocalDate;

/**
 * Optional search inputs for Stage 6.
 * Any null/blank field is treated as "not filtered".
 */
public record SearchFilter(
        String companyContains,
        String positionContains,
        String locationContains,
        ApplicationStatus status,
        LocalDate dateFrom,
        LocalDate dateTo,
        SortField sortField,
        SortDirection sortDirection
) { }
