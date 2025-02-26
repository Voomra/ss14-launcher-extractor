package ru.di9.jdbc;

import org.intellij.lang.annotations.Language;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface JdbcTemplate {

    void execute(@Language("GenericSQL") String sql) throws DataAccessException;

    void execute(@Language("GenericSQL") String sql, PreparedStatementProcessor psp) throws DataAccessException;

    <T> T query(@Language("GenericSQL") String sql, ResultSetExtractor<T> rse) throws DataAccessException;

    <T> T query(@Language("GenericSQL") String sql, PreparedStatementProcessor psp, ResultSetExtractor<T> rse)
            throws DataAccessException;

    <T> Optional<T> queryOne(@Language("GenericSQL") String sql, ResultSetExtractor<T> rse)
            throws DataAccessException;

    <T> Optional<T> queryOne(@Language("GenericSQL") String sql, PreparedStatementProcessor psp,
                             ResultSetExtractor<T> rse) throws DataAccessException;

    <T> List<T> queryList(@Language("GenericSQL") String sql,
                          final RowMapper<T> rowMapper) throws DataAccessException;

    <T> List<T> queryList(@Language("GenericSQL") String sql, PreparedStatementProcessor psp,
                          final RowMapper<T> rowMapper) throws DataAccessException;

    <T> T insert(@Language("GenericSQL") String sql, PreparedStatementProcessor psp,
                 ResultSetExtractor<T> processGeneratedKey) throws DataAccessException;

    void transaction(Consumer<JdbcTemplate> consumer);
}
