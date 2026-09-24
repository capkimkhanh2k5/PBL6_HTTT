package com.danasea.backend;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlMigrationIntegrationTest {

    @Container
    static final GenericContainer<?> POSTGRES = new GenericContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withEnv("POSTGRES_DB", "migration_test")
            .withEnv("POSTGRES_USER", "migration_user")
            .withEnv("POSTGRES_PASSWORD", "migration_password")
            .withExposedPorts(5432)
            .waitingFor(Wait.forListeningPort());

    @Test
    void allMigrationsApplyAndValidateOnPostgreSql16() throws Exception {
        String jdbcUrl = "jdbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getFirstMappedPort() + "/migration_test";

        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, "migration_user", "migration_password")
                .locations("classpath:db/migration")
                .load();

        var migrationResult = flyway.migrate();
        assertTrue(migrationResult.success);
        assertNotNull(flyway.info().current());
        assertEquals("10", flyway.info().current().getVersion().getVersion());
        assertTrue(flyway.validateWithResult().validationSuccessful);

        try (var connection = DriverManager.getConnection(jdbcUrl, "migration_user", "migration_password");
             var statement = connection.createStatement()) {
            assertTrue(tableExists(statement, "refunds"));
            assertTrue(tableExists(statement, "settlements"));
            assertTrue(tableExists(statement, "checkin_tokens"));
            assertTrue(tableExists(statement, "category_safety_rules"));
        }
    }

    private boolean tableExists(java.sql.Statement statement, String tableName) throws Exception {
        try (var resultSet = statement.executeQuery(
                "SELECT EXISTS (SELECT 1 FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = '" + tableName + "')")) {
            return resultSet.next() && resultSet.getBoolean(1);
        }
    }
}
