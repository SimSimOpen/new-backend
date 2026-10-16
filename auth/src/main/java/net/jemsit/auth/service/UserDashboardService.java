package net.jemsit.auth.service;

import lombok.RequiredArgsConstructor;
import net.jemsit.common.dto.response.dashboard.ActivityItemDTO;
import net.jemsit.common.dto.response.dashboard.MonthlyCountDTO;
import net.jemsit.common.dto.response.dashboard.UserDashboardDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Account aggregates for the admin dashboard. {@code authorities} is a varchar[], hence native SQL. */
@Service
@RequiredArgsConstructor
public class UserDashboardService {

    private static final int MONTHS = 12;
    private static final int ACTIVITY = 10;

    private final NamedParameterJdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public UserDashboardDTO getUserDashboard() {
        YearMonth now = YearMonth.now();
        var params = new MapSqlParameterSource()
                .addValue("since", now.minusMonths(MONTHS - 1).atDay(1).atStartOfDay())
                .addValue("monthStart", now.atDay(1).atStartOfDay());

        Map<String, Object> totals = jdbc.queryForMap("""
                SELECT COUNT(*) FILTER (WHERE 'CLIENT' = ANY(authorities)) AS clients,
                       COUNT(*) FILTER (WHERE 'AGENT' = ANY(authorities)) AS agents,
                       COUNT(*) FILTER (WHERE 'CLIENT' = ANY(authorities) AND created_at >= :monthStart) AS new_clients
                FROM users WHERE is_deleted = false
                """, params);

        Map<String, Long> perMonth = new HashMap<>();
        jdbc.query("""
                SELECT to_char(date_trunc('month', created_at), 'YYYY-MM') AS month, COUNT(*) AS count
                FROM users WHERE 'CLIENT' = ANY(authorities) AND created_at >= :since
                GROUP BY 1
                """, params, rs -> {
            perMonth.put(rs.getString("month"), rs.getLong("count"));
        });

        // Sign-ups, agent creations and agent deactivations, newest first. Admin accounts are left out:
        // they are created by the system, not by anyone using it.
        List<ActivityItemDTO> activity = new ArrayList<>(jdbc.query("""
                SELECT id, username, 'AGENT' = ANY(authorities) AS agent, created_at FROM users
                WHERE NOT ('ADMIN' = ANY(authorities))
                ORDER BY created_at DESC LIMIT
                """ + " " + ACTIVITY, params, (rs, i) -> new ActivityItemDTO(
                rs.getBoolean("agent") ? "AGENT_CREATED" : "USER_REGISTERED", rs.getLong("id"), null,
                rs.getString("username"), null, rs.getTimestamp("created_at").toLocalDateTime())));
        activity.addAll(jdbc.query("""
                SELECT id, username, deleted_at FROM users
                WHERE 'AGENT' = ANY(authorities) AND is_deleted = true AND deleted_at IS NOT NULL
                ORDER BY deleted_at DESC LIMIT
                """ + " " + ACTIVITY, params, (rs, i) -> new ActivityItemDTO(
                "AGENT_DEACTIVATED", rs.getLong("id"), null, rs.getString("username"), null,
                rs.getTimestamp("deleted_at").toLocalDateTime())));
        activity.sort(Comparator.comparing(ActivityItemDTO::at).reversed());

        return new UserDashboardDTO(
                number(totals, "clients"), number(totals, "agents"), number(totals, "new_clients"),
                MonthlyCountDTO.lastMonths(MONTHS, perMonth),
                activity.subList(0, Math.min(ACTIVITY, activity.size())));
    }

    private static long number(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value == null ? 0 : ((Number) value).longValue();
    }
}
