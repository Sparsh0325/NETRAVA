package db.migration;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class V7RehashAuditChainTest {
    @Test
    void rehashesExistingRowsInOrderAndUpdatesChainHead() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:h2:mem:rehash;DB_CLOSE_DELAY=-1")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE audit_logs (id BIGINT PRIMARY KEY, chain_index BIGINT UNIQUE, timestamp TIMESTAMP WITH TIME ZONE, actor_username VARCHAR(100), actor_role VARCHAR(50), actor_badge_number VARCHAR(50), agency VARCHAR(100), action VARCHAR(100), resource VARCHAR(255), details TEXT, previous_hash VARCHAR(64), hash VARCHAR(64), hash_version INTEGER DEFAULT 1)");
                statement.execute("CREATE TABLE audit_chain_state (id INTEGER PRIMARY KEY, last_index BIGINT, last_hash VARCHAR(64))");
                statement.execute("INSERT INTO audit_chain_state VALUES (1, 2, 'old-head')");
            }
            String old = "a".repeat(64);
            try (var insert = connection.prepareStatement("INSERT INTO audit_logs VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                insert.setLong(1, 1); insert.setLong(2, 1); insert.setObject(3, Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")));
                insert.setString(4, "Officer"); insert.setString(5, "Officer"); insert.setString(6, "B-1"); insert.setString(7, "Agency");
                insert.setString(8, "CASE_CREATE"); insert.setString(9, "case:A"); insert.setString(10, "first\nentry");
                insert.setString(11, "0".repeat(64)); insert.setString(12, old); insert.setInt(13, 1); insert.executeUpdate();
                insert.setLong(1, 2); insert.setLong(2, 2); insert.setObject(3, Timestamp.from(Instant.parse("2026-01-01T00:00:01Z")));
                insert.setString(4, "Officer"); insert.setString(5, "Officer"); insert.setString(6, "B-1"); insert.setString(7, "Agency");
                insert.setString(8, "CASE_UPDATE"); insert.setString(9, "case:A"); insert.setString(10, "second entry");
                insert.setString(11, old); insert.setString(12, old); insert.setInt(13, 1); insert.executeUpdate();
            }
            Context context = mock(Context.class); when(context.getConnection()).thenReturn(connection);
            new V7__rehash_audit_chain().migrate(context);
            try (var rows = connection.createStatement().executeQuery("SELECT chain_index,previous_hash,hash,hash_version FROM audit_logs ORDER BY chain_index")) {
                assertTrue(rows.next()); String firstHash = rows.getString("hash");
                assertEquals("0".repeat(64), rows.getString("previous_hash")); assertEquals(2, rows.getInt("hash_version")); assertNotEquals(old, firstHash);
                assertTrue(rows.next()); assertEquals(firstHash, rows.getString("previous_hash")); assertEquals(2, rows.getInt("hash_version"));
                String finalHash = rows.getString("hash"); assertFalse(rows.next());
                try (var state = connection.createStatement().executeQuery("SELECT last_index,last_hash FROM audit_chain_state WHERE id=1")) {
                    assertTrue(state.next()); assertEquals(2, state.getLong(1)); assertEquals(finalHash, state.getString(2));
                }
            }
        }
    }
}
