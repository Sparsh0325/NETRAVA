package db.migration;

import com.intellishare.audit.AuditService;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import java.sql.Timestamp;
import java.time.OffsetDateTime;

/** Rehashes existing audit records in their original chain order using the v2 unambiguous encoding. */
public class V7__rehash_audit_chain extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        String previous;
        try (var select = connection.prepareStatement("SELECT previous_hash FROM audit_logs ORDER BY chain_index LIMIT 1"); var rows = select.executeQuery()) {
            previous = rows.next() ? rows.getString(1) : "0".repeat(64);
        }
        try (var select = connection.prepareStatement("SELECT id,chain_index,timestamp,actor_username,actor_role,actor_badge_number,agency,action,resource,details FROM audit_logs ORDER BY chain_index");
             var rows = select.executeQuery();
             var update = connection.prepareStatement("UPDATE audit_logs SET previous_hash=?, hash=?, hash_version=2 WHERE id=?")) {
            long lastIndex = 0;
            String lastHash = previous;
            while (rows.next()) {
                long id = rows.getLong("id"), index = rows.getLong("chain_index");
                Timestamp timestamp = rows.getTimestamp("timestamp");
                OffsetDateTime at = timestamp.toInstant().atOffset(java.time.ZoneOffset.UTC);
                String hash = AuditService.hashV2(index, at, rows.getString("actor_username"), rows.getString("actor_role"),
                        rows.getString("actor_badge_number"), rows.getString("agency"), rows.getString("action"),
                        rows.getString("resource"), rows.getString("details"), previous);
                update.setString(1, previous); update.setString(2, hash); update.setLong(3, id); update.addBatch();
                previous = hash; lastHash = hash; lastIndex = index;
            }
            update.executeBatch();
            try (var state = connection.prepareStatement("UPDATE audit_chain_state SET last_index=?, last_hash=? WHERE id=1")) {
                state.setLong(1, lastIndex); state.setString(2, lastHash); state.executeUpdate();
            }
        }
    }
}
