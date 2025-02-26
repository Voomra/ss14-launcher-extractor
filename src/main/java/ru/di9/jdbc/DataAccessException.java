package ru.di9.jdbc;

import lombok.Getter;

@Getter
public class DataAccessException extends RuntimeException {

    @SuppressWarnings("java:S1165")
    private String sql;

    public DataAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public DataAccessException(String msg, String sql, Throwable cause) {
        this(msg + " | " + sql, cause);
        this.sql = sql;
    }
}
