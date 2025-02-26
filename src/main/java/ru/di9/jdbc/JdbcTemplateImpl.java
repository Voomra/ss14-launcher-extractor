package ru.di9.jdbc;

import org.intellij.lang.annotations.Language;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class JdbcTemplateImpl implements JdbcTemplate {
    private final DataSource dataSource;

    public JdbcTemplateImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void execute(@Language("GenericSQL") String sql) throws DataAccessException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw throwDataAccessException(sql, e);
        }
    }

    @Override
    public void execute(@Language("GenericSQL") String sql, PreparedStatementProcessor psp) throws DataAccessException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            psp.process(preparedStatement);
            preparedStatement.execute();
        } catch (SQLException e) {
            throw throwDataAccessException(sql, e);
        }
    }

    @Override
    public <T> T query(@Language("GenericSQL") String sql, ResultSetExtractor<T> rse) throws DataAccessException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return rse.extractData(resultSet);
        } catch (SQLException e) {
            throw throwDataAccessException(sql, e);
        }
    }

    @Override
    public <T> T query(@Language("GenericSQL") String sql, PreparedStatementProcessor psp,
                       ResultSetExtractor<T> rse) throws DataAccessException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            psp.process(preparedStatement);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return rse.extractData(resultSet);
            }
        } catch (SQLException e) {
            throw throwDataAccessException(sql, e);
        }
    }

    @Override
    public <T> Optional<T> queryOne(@Language("GenericSQL") String sql,
                                    ResultSetExtractor<T> rse) throws DataAccessException {
        return query(sql, rs -> {
            if (rs.next()) {
                return Optional.ofNullable(rse.extractData(rs));
            } else {
                return Optional.empty();
            }
        });
    }

    @Override
    public <T> Optional<T> queryOne(@Language("GenericSQL") String sql, PreparedStatementProcessor psp,
                                    ResultSetExtractor<T> rse) throws DataAccessException {
        return query(sql, psp, rs -> {
            if (rs.next()) {
                return Optional.ofNullable(rse.extractData(rs));
            } else {
                return Optional.empty();
            }
        });
    }

    @Override
    public <T> List<T> queryList(@Language("GenericSQL") String sql,
                                 final RowMapper<T> rowMapper) throws DataAccessException {
        return query(sql, createResultSetExtractorList(rowMapper));
    }

    @Override
    public <T> List<T> queryList(@Language("GenericSQL") String sql, PreparedStatementProcessor psp,
                                 final RowMapper<T> rowMapper) throws DataAccessException {
        return query(sql, psp, createResultSetExtractorList(rowMapper));
    }

    @Override
    public <T> T insert(@Language("GenericSQL") String sql, PreparedStatementProcessor psp,
                        ResultSetExtractor<T> processGeneratedKey) throws DataAccessException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.createStatement().execute("BEGIN TRANSACTION");
            consumer.accept(new JdbcTemplateTransactional(connection));
            connection.createStatement().execute("COMMIT");
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.createStatement().execute("ROLLBACK");
                    throw new DataAccessException("Error transaction", e);
                } catch (SQLException e1) {
                    DataAccessException exception = new DataAccessException("Error rollback", e1);
                    exception.addSuppressed(e1);
                    throw exception;
                }
            }
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    throw new DataAccessException("Error close connection", e);
                }
            }
        }
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

    static DataAccessException throwDataAccessException(String sql, Exception e) {
        return new DataAccessException("Error execute SQL", sql, e);
    }
}
