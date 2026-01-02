package com.johndoan.jobtracker;

public enum SortField {
    ID("id"),
    COMPANY("company"),
    POSITION("position"),
    LOCATION("location"),
    STATUS("status"),
    DATE_APPLIED("date_applied");

    private final String column;

    SortField(String column) {
        this.column = column;
    }

    public String column() {
        return column;
    }
}
