package ru.di9.jdbc;

import org.intellij.lang.annotations.Language;

import java.util.List;

public interface JdbcTemplate {

    <T> T query(@Language("GenericSQL") String sql, ResultSetExtractor<T> rse) throws DataAccessException;

    <T> void query(@Language("GenericSQL") String sql, PreparedStatementProcessor psp, ResultSetExtractor<T> rse)
            throws DataAccessException;

    <T> List<T> queryList(@Language("GenericSQL") String sql,
                          final RowMapper<T> rowMapper) throws DataAccessException;

}
