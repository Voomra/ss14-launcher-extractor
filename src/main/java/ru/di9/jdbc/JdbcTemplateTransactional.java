package ru.di9.jdbc;

import lombok.RequiredArgsConstructor;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static ru.di9.jdbc.JdbcTemplateImpl.throwDataAccessException;

@RequiredArgsConstructor
public class JdbcTemplateTransactional implements JdbcTemplate, AutoCloseable {
    private final Connection connection;

    @Override
    public void close() throws Exception {
        connection.close();
    }

    @Override
    public void execute(String sql) throws DataAccessException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw throwDataAccessException(sql, e);
        }
    }

    @Override
    public void execute(String sql, PreparedStatementProcessor psp) throws DataAccessException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            psp.process(preparedStatement);
            preparedStatement.execute();
        } catch (SQLException e) {
            throw throwDataAccessException(sql, e);
        }
    }

    @Override
    public <T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return rse.extractData(resultSet);
        } catch (SQLException e) {
            throw throwDataAccessException(sql, e);
        }
    }

    @Override
    public <T> T query(String sql, PreparedStatementProcessor psp, ResultSetExtractor<T> rse) throws DataAccessException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            psp.process(preparedStatement);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return rse.extractData(resultSet);
            }
        } catch (SQLException e) {
            throw throwDataAccessException(sql, e);
        }
    }

    @Override
    public <T> Optional<T> queryOne(String sql, ResultSetExtractor<T> rse) throws DataAccessException {
        return query(sql, rs -> {
            if (rs.next()) {
                return Optional.ofNullable(rse.extractData(rs));
            } else {
                return Optional.empty();
            }
        });
    }

    @Override
    public <T> Optional<T> queryOne(String sql, PreparedStatementProcessor psp, ResultSetExtractor<T> rse) throws DataAccessException {
        return query(sql, psp, rs -> {
            if (rs.next()) {
                return Optional.ofNullable(rse.extractData(rs));
            } else {
                return Optional.empty();
            }
        });
    }

    @Override
    public <T> List<T> queryList(String sql, RowMapper<T> rowMapper) throws DataAccessException {
        return query(sql, createResultSetExtractorList(rowMapper));
    }

    @Override
    public <T> List<T> queryList(String sql, PreparedStatementProcessor psp, RowMapper<T> rowMapper) throws DataAccessException {
        return query(sql, psp, createResultSetExtractorList(rowMapper));
    }

    @Override
    public <T> T insert(String sql, PreparedStatementProcessor psp, ResultSetExtractor<T> processGeneratedKey) throws DataAccessException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            psp.process(preparedStatement);
            preparedStatement.execute();
            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                generatedKeys.next();
                return processGeneratedKey.extractData(generatedKeys);
            }
        } catch (SQLException e) {
            throw throwDataAccessException(sql, e);
        }
    }

    @Override
    public void transaction(Consumer<JdbcTemplate> consumer) {
        consumer.accept(this);
    }

    private <T> ResultSetExtractor<List<T>> createResultSetExtractorList(final RowMapper<T> rowMapper) {
        return rs -> {
            List<T> resultList;
            int rowNum = 0;
            if (rs.next()) {
                resultList = new ArrayList<>();

                do {
                    resultList.add(rowMapper.mapRow(rs, rowNum++));
                } while (rs.next());
            } else {
                resultList = Collections.emptyList();
            }

            return resultList;
        };
    }
}
