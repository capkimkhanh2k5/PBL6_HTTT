package com.danasea.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.DriverManager;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

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

        Flyway legacy = Flyway.configure()
                .dataSource(jdbcUrl, "migration_user", "migration_password")
                .locations("classpath:db/migration").target("17").load();
        legacy.migrate();
        java.util.UUID legacyPayment = java.util.UUID.randomUUID();
        try (var connection = DriverManager.getConnection(jdbcUrl, "migration_user", "migration_password");
             var statement = connection.prepareStatement("INSERT INTO payments(id, status, created_at, updated_at) VALUES (?, 'REFUNDED', TIMESTAMPTZ '2026-09-01 10:00:00+07', TIMESTAMPTZ '2026-09-02 10:00:00+07')")) {
            statement.setObject(1, legacyPayment);
            statement.executeUpdate();
        }
        var migrationResult = flyway.migrate();
        assertTrue(migrationResult.success);
        assertNotNull(flyway.info().current());
        assertEquals("22", flyway.info().current().getVersion().getVersion());
        assertTrue(flyway.validateWithResult().validationSuccessful);

        try (var connection = DriverManager.getConnection(jdbcUrl, "migration_user", "migration_password");
             var statement = connection.createStatement()) {
            try (var row = statement.executeQuery("SELECT paid_at IS NOT NULL FROM payments WHERE id = '" + legacyPayment + "'")) {
                assertTrue(row.next() && row.getBoolean(1));
            }
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
