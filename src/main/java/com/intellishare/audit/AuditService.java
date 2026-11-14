package com.intellishare.audit;

import com.intellishare.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class AuditService {
    private static final String GENESIS = "0".repeat(64);
    private final JdbcTemplate jdbc;

    public AuditService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public void record(User actor, String action, String resource, String details) {
        // PostgreSQL stores timestamptz at microsecond precision; hash the same precision it persists.
        OffsetDateTime at = OffsetDateTime.now(java.time.ZoneOffset.UTC).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var state = jdbc.queryForMap("SELECT last_index, last_hash FROM audit_chain_state WHERE id = 1 FOR UPDATE");
        long index = ((Number) state.get("last_index")).longValue() + 1;
        String previous = (String) state.get("last_hash");
        String username = actor.getName();
        String role = actor.getRole();
        String badge = actor.getBadgeNumber();
        String agency = actor.getAgency();
        String hash = hashV2(index, at, username, role, badge, agency, action, resource, details, previous);
        jdbc.update("INSERT INTO audit_logs(chain_index,timestamp,actor_username,actor_role,actor_badge_number,agency,action,resource,details,previous_hash,hash,hash_version) VALUES(?,?,?,?,?,?,?,?,?,?,?,2)",
                index, at, username, role, badge, agency, action, resource, details, previous, hash);
        jdbc.update("UPDATE audit_chain_state SET last_index=?, last_hash=? WHERE id=1", index, hash);
    }

    @Transactional(readOnly = true)
    public AuditPage list(User actor, int page, int size, String query) {
        validatePage(page, size);
        boolean admin = "Administrator".equals(actor.getRole());
        String scope = admin ? "" : " AND agency = ?";
        String search = query == null || query.isBlank() ? "" : " AND (action ILIKE ? OR resource ILIKE ? OR actor_username ILIKE ?)";
        Object[] args = arguments(admin, actor, query);
        Long total = jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE 1=1" + scope + search, Long.class, args);
        Object[] pageArgs = java.util.Arrays.copyOf(args, args.length + 2);
        pageArgs[args.length] = size;
        pageArgs[args.length + 1] = (long) page * size;
        List<AuditEvent> rows = jdbc.query("SELECT id,chain_index,timestamp,actor_username,actor_role,actor_badge_number,agency,action,resource,details,previous_hash,hash,hash_version FROM audit_logs WHERE 1=1" + scope + search + " ORDER BY chain_index DESC LIMIT ? OFFSET ?", (rs, n) -> new AuditEvent(
                rs.getLong("id"), rs.getLong("chain_index"), rs.getObject("timestamp", OffsetDateTime.class), rs.getString("actor_username"),
                rs.getString("actor_role"), rs.getString("actor_badge_number"), rs.getString("agency"), rs.getString("action"), rs.getString("resource"),
                rs.getString("details"), rs.getString("previous_hash"), rs.getString("hash"), rs.getInt("hash_version")), pageArgs);
        long count = total == null ? 0 : total;
        return new AuditPage(rows, page, size, count, (int) ((count + size - 1) / size));
    }

    @Transactional(readOnly = true)
    public AuditPage listOwn(User actor, int page, int size) {
        validatePage(page, size);
        String badge = actor.getBadgeNumber();
        Long total = jdbc.queryForObject("SELECT count(*) FROM audit_logs WHERE actor_badge_number = ?", Long.class, badge);
        List<AuditEvent> rows = jdbc.query("SELECT id,chain_index,timestamp,actor_username,actor_role,actor_badge_number,agency,action,resource,details,previous_hash,hash,hash_version FROM audit_logs WHERE actor_badge_number = ? ORDER BY chain_index DESC LIMIT ? OFFSET ?", (rs, n) -> new AuditEvent(
                rs.getLong("id"), rs.getLong("chain_index"), rs.getObject("timestamp", OffsetDateTime.class), rs.getString("actor_username"),
                rs.getString("actor_role"), rs.getString("actor_badge_number"), rs.getString("agency"), rs.getString("action"), rs.getString("resource"),
                rs.getString("details"), rs.getString("previous_hash"), rs.getString("hash"), rs.getInt("hash_version")), badge, size, (long) page * size);
        long count = total == null ? 0 : total;
        return new AuditPage(rows, page, size, count, (int) ((count + size - 1) / size));
    }

    @Transactional(readOnly = true)
    public String exportCsv(User actor) {
        StringBuilder csv = new StringBuilder("timestamp,actor,role,agency,action,resource,details,chain_index,hash\r\n");
        for (int pageNumber = 0; ; pageNumber++) {
            AuditPage page = list(actor, pageNumber, 1000, null);
            for (AuditEvent e : page.content()) csv.append(cell(e.timestamp().toString())).append(',').append(cell(e.actorUsername())).append(',')
                    .append(cell(e.actorRole())).append(',').append(cell(e.agency())).append(',').append(cell(e.action())).append(',')
                    .append(cell(e.resource())).append(',').append(cell(e.details())).append(',').append(e.chainIndex()).append(',').append(cell(e.hash())).append("\r\n");
            if (pageNumber + 1 >= page.totalPages()) break;
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public String exportOwnCsv(User actor) {
        StringBuilder csv = new StringBuilder("timestamp,actor,role,agency,action,resource,details,chain_index,hash\r\n");
        for (int pageNumber = 0; ; pageNumber++) {
            AuditPage page = listOwn(actor, pageNumber, 1000);
            for (AuditEvent e : page.content()) csv.append(cell(e.timestamp().toString())).append(',').append(cell(e.actorUsername())).append(',')
                    .append(cell(e.actorRole())).append(',').append(cell(e.agency())).append(',').append(cell(e.action())).append(',')
                    .append(cell(e.resource())).append(',').append(cell(e.details())).append(',').append(e.chainIndex()).append(',').append(cell(e.hash())).append("\r\n");
            if (pageNumber + 1 >= page.totalPages()) break;
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public boolean verify() {
        List<AuditEvent> rows = jdbc.query("SELECT id,chain_index,timestamp,actor_username,actor_role,actor_badge_number,agency,action,resource,details,previous_hash,hash,hash_version FROM audit_logs ORDER BY chain_index", (rs, n) -> new AuditEvent(
                rs.getLong("id"), rs.getLong("chain_index"), rs.getObject("timestamp", OffsetDateTime.class), rs.getString("actor_username"), rs.getString("actor_role"),
                rs.getString("actor_badge_number"), rs.getString("agency"), rs.getString("action"), rs.getString("resource"), rs.getString("details"), rs.getString("previous_hash"), rs.getString("hash"), rs.getInt("hash_version")));
        String previous = rows.isEmpty() ? GENESIS : rows.getFirst().previousHash();
        long index = rows.isEmpty() ? 0 : rows.getFirst().chainIndex() - 1;
        for (AuditEvent e : rows) {
            String expected = e.hashVersion() == 2
                    ? hashV2(e.chainIndex(), e.timestamp(), e.actorUsername(), e.actorRole(), e.actorBadgeNumber(), e.agency(), e.action(), e.resource(), e.details(), previous)
                    : e.hashVersion() == 1 ? hashV1(e.chainIndex(), e.timestamp(), e.actorUsername(), e.actorRole(), e.actorBadgeNumber(), e.agency(), e.action(), e.resource(), e.details(), previous) : null;
            if (e.chainIndex() != ++index || !previous.equals(e.previousHash()) || expected == null || !expected.equals(e.hash())) return false;
            previous = e.hash();
        }
        return true;
    }

    private Object[] arguments(boolean admin, User actor, String query) {
        var values = new java.util.ArrayList<Object>();
        if (!admin) values.add(actor.getAgency());
        if (query != null && !query.isBlank()) { String pattern = "%" + query.trim() + "%"; values.add(pattern); values.add(pattern); values.add(pattern); }
        return values.toArray();
    }
    private void validatePage(int page, int size) { if (page < 0 || size < 1 || size > 1000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be 1-1000"); }
    private String cell(String value) {
        String safe = value == null ? "" : value;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
    public static String hashV2(long index, OffsetDateTime at, String actor, String role, String badge, String agency, String action, String resource, String details, String previous) {
        return digest(String.join(".", "2", Long.toString(index), at.toInstant().toString(), b64(actor), b64(role), b64(badge), b64(agency), b64(action), b64(resource), b64(details), previous));
    }
    static String hashV1(long index, OffsetDateTime at, String actor, String role, String badge, String agency, String action, String resource, String details, String previous) {
        String canonical = String.join("\n", Long.toString(index), at.toString(), n(actor), n(role), n(badge), n(agency), n(action), n(resource), n(details), previous);
        return digest(canonical);
    }
    private static String digest(String canonical) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static String b64(String value) { return value == null ? "~" : java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private static String n(String value) { return value == null ? "" : value; }
}
