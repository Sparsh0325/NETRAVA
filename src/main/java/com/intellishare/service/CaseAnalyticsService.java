package com.intellishare.service;

import com.intellishare.dto.dashboard.CaseAnalyticsResponse;
import com.intellishare.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CaseAnalyticsService {
    private final JdbcTemplate jdbc;
    public CaseAnalyticsService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(readOnly = true)
    public CaseAnalyticsResponse analytics(User actor) {
        boolean admin = "Administrator".equals(actor.getRole());
        String where = admin ? "" : " WHERE agency = ?";
        Object[] scope = admin ? new Object[0] : new Object[]{actor.getAgency()};
        long total = jdbc.queryForObject("SELECT count(*) FROM cases" + where, Long.class, scope);
        return new CaseAnalyticsResponse(total,
                buckets("SELECT case_type, count(*) FROM cases" + where + " GROUP BY case_type ORDER BY count(*) DESC", scope),
                buckets("SELECT status, count(*) FROM cases" + where + " GROUP BY status ORDER BY count(*) DESC", scope),
                buckets("SELECT priority, count(*) FROM cases" + where + " GROUP BY priority ORDER BY count(*) DESC", scope),
                buckets("SELECT to_char(date_trunc('month', incident_date)::date, 'YYYY-MM'), count(*) FROM cases" + (admin ? " WHERE incident_date IS NOT NULL" : " WHERE agency = ? AND incident_date IS NOT NULL") + " GROUP BY 1 ORDER BY 1", scope));
    }

    private List<CaseAnalyticsResponse.Bucket> buckets(String sql, Object[] args) {
        return jdbc.query(sql, (rs, row) -> new CaseAnalyticsResponse.Bucket(rs.getString(1), rs.getLong(2)), args);
    }
}
