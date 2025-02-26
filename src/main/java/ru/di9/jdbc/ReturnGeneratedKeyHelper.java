package ru.di9.jdbc;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReturnGeneratedKeyHelper {
    public static final ResultSetExtractor<Long> RETURN_GENERATED_KEY_FIRST_LONG = rs -> rs.getLong(1);
    public static final ResultSetExtractor<Integer> RETURN_GENERATED_KEY_FIRST_INT = rs -> rs.getInt(1);
}
