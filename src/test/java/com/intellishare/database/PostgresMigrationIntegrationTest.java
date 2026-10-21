package com.intellishare.database;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PostgresMigrationIntegrationTest {

    private record DbConfig(String url, String user, String password) {}

    private DbConfig resolveConfig() {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");

        if ((url == null || password == null) && Files.exists(Path.of(".env"))) {
            try {
                List<String> lines = Files.readAllLines(Path.of(".env"));
                Map<String, String> map = new HashMap<>();
                for (String line : lines) {
                    line = line.trim();
                    if (!line.startsWith("#") && line.contains("=")) {
                        int idx = line.indexOf('=');
                        map.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                    }
                }
                if (url == null) url = map.get("DB_URL");
                if (user == null) user = map.get("DB_USERNAME");
                if (password == null) password = map.get("DB_PASSWORD");
            } catch (IOException ignored) {
            }
        }

        if (url == null) {
            url = "jdbc:postgresql://127.0.0.1:5436/intellishare";
        }
        if (user == null) {
            user = "intellishare_user";
        }
        return new DbConfig(url, user, password != null ? password : "");
    }

    @Test
    @DisplayName("Verify Flyway migration executes against real PostgreSQL and creates expected tables")
    void testFlywayMigrationAgainstRealPostgres() throws Exception {
        DbConfig config = resolveConfig();

        // 1. Run Flyway migration
        Flyway flyway = Flyway.configure()
                .dataSource(config.url(), config.user(), config.password())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        MigrateResult result = flyway.migrate();
        assertTrue(result.success, "Flyway migration should succeed");

        // 2. Query information_schema.tables to verify all tables exist
        Set<String> expectedTables = Set.of(
                "flyway_schema_history",
                "users",
                "cases",
                "case_entities",
                "case_evidence",
                "case_timelines",
                "criminal_records",
                "audit_logs",
                "audit_chain_state",
                "ingestion_jobs",
                "case_shares"
        );

        Set<String> actualTables = new HashSet<>();
        try (Connection conn = DriverManager.getConnection(config.url(), config.user(), config.password());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")) {

            while (rs.next()) {
                actualTables.add(rs.getString("table_name"));
            }
        }

        for (String expected : expectedTables) {
            assertTrue(actualTables.contains(expected), "Database should contain table: " + expected);
        }
    }
}
