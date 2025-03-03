package ru.di9.jdbc;

import org.intellij.lang.annotations.Language;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("SqlSourceToSinkFlow")
public class JdbcTemplateImpl implements JdbcTemplate {
    private final DataSource dataSource;

    public JdbcTemplateImpl(DataSource dataSource) {
        this.dataSource = dataSource;
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
    public <T> void query(@Language("GenericSQL") String sql, PreparedStatementProcessor psp,
                          ResultSetExtractor<T> rse) throws DataAccessException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            psp.process(preparedStatement);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                rse.extractData(resultSet);
            }
        } catch (SQLException e) {
            throw throwDataAccessException(sql, e);
        }
    }

    @Override
    public <T> List<T> queryList(@Language("GenericSQL") String sql,
                                 final RowMapper<T> rowMapper) throws DataAccessException {
        return query(sql, createResultSetExtractorList(rowMapper));
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
