package com.auction.app.connection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@DataJpaTest
@SqlGroup({
        @Sql(scripts = "/test-init.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS),
        @Sql(statements = "DROP TABLE IF EXISTS tests", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
})
public class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testConnection() throws SQLException {
        Assertions.assertNotNull(dataSource);
        try (Connection connection = dataSource.getConnection()) {
            Assertions.assertNotNull(connection);
            Assertions.assertFalse(connection.isClosed());
        }
    }

    @Test
    void testSchema() {
        Integer rowCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tests", Integer.class);
        Assertions.assertEquals(4, rowCount);
    }
}
