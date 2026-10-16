package net.jemsit.product.service;

import lombok.RequiredArgsConstructor;
import net.jemsit.common.UserContext;
import net.jemsit.common.data.enums.Roles;
import net.jemsit.common.dto.response.dashboard.ActivityItemDTO;
import net.jemsit.common.dto.response.dashboard.ListingDashboardDTO;
import net.jemsit.common.dto.response.dashboard.ListingDashboardDTO.TopListing;
import net.jemsit.common.dto.response.dashboard.ListingDashboardDTO.TypeCount;
import net.jemsit.common.dto.response.dashboard.MonthlyCountDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only aggregates for the admin dashboard. Plain SQL, because these are reporting queries over
 * the whole table rather than entity loads. Agents see their own listings; admins see everything.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int MONTHS = 12;
    private static final int MOST_VIEWED = 5;
    private static final int ACTIVITY = 10;
    /** An edit this soon after creation is still part of creating it (drafts are saved right away). */
    private static final Duration CREATION_WINDOW = Duration.ofMinutes(2);

    /** Appended to every query: live rows, optionally only the caller's. */
    private static final String SCOPE = " is_deleted = false AND (CAST(:agentId AS bigint) IS NULL OR agentid = :agentId) ";

    private final NamedParameterJdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public ListingDashboardDTO getListingDashboard() {
        var params = new MapSqlParameterSource("agentId", agentScope())
                .addValue("since", YearMonth.now().minusMonths(MONTHS - 1).atDay(1).atStartOfDay());

        Map<String, Object> totals = jdbc.queryForMap("""
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE listing_status = 'ACTIVE') AS active,
                       COUNT(*) FILTER (WHERE listing_status = 'DRAFT') AS draft,
                       COUNT(*) FILTER (WHERE listing_status = 'ARCHIVED') AS archived,
                       COUNT(*) FILTER (WHERE occupancy_status = 'AVAILABLE') AS available,
                       COUNT(*) FILTER (WHERE occupancy_status = 'RENTED') AS rented,
                       COUNT(*) FILTER (WHERE occupancy_status = 'SOLD') AS sold,
                       COALESCE(SUM(view_count), 0) AS views
                FROM properties WHERE""" + SCOPE, params);

        Map<String, Long> perMonth = new HashMap<>();
        jdbc.query("""
                SELECT to_char(date_trunc('month', created_at), 'YYYY-MM') AS month, COUNT(*) AS count
                FROM properties WHERE created_at >= :since AND""" + SCOPE + """
                GROUP BY 1""", params, rs -> {
            perMonth.put(rs.getString("month"), rs.getLong("count"));
        });

        List<TypeCount> byType = jdbc.query("""
                SELECT type, COUNT(*) AS count FROM properties
                WHERE type IS NOT NULL AND""" + SCOPE + """
                GROUP BY type ORDER BY count DESC, type""", params,
                (rs, i) -> new TypeCount(rs.getString("type"), rs.getLong("count")));

        List<TopListing> mostViewed = jdbc.query("""
                SELECT id, title, view_count, listing_status FROM properties
                WHERE view_count > 0 AND""" + SCOPE + """
                ORDER BY view_count DESC, id LIMIT""" + " " + MOST_VIEWED, params,
                (rs, i) -> new TopListing(rs.getLong("id"), rs.getString("title"), rs.getLong("view_count"),
                        rs.getString("listing_status")));

        List<ActivityItemDTO> activity = jdbc.query("""
                SELECT id, title, agent, listing_status, created_at, updated_at FROM properties
                WHERE""" + SCOPE + """
                ORDER BY COALESCE(updated_at, created_at) DESC LIMIT""" + " " + ACTIVITY, params,
                (rs, i) -> toActivity(rs.getLong("id"), rs.getString("title"), rs.getString("agent"),
                        rs.getString("listing_status"), rs.getTimestamp("created_at"), rs.getTimestamp("updated_at")));

        return new ListingDashboardDTO(
                number(totals, "total"), number(totals, "active"), number(totals, "draft"),
                number(totals, "archived"), number(totals, "available"), number(totals, "rented"),
                number(totals, "sold"), number(totals, "views"),
                MonthlyCountDTO.lastMonths(MONTHS, perMonth), byType, mostViewed, activity);
    }

    /**
     * Listings keep no history, so the feed is derived from their timestamps: a row that has not been
     * touched since it was created is a creation, anything else is its latest update.
     */
    private static ActivityItemDTO toActivity(long id, String title, String agent, String status,
                                              Timestamp created, Timestamp updated) {
        LocalDateTime createdAt = created.toLocalDateTime();
        LocalDateTime updatedAt = updated == null ? null : updated.toLocalDateTime();
        boolean justCreated = updatedAt == null || Duration.between(createdAt, updatedAt).compareTo(CREATION_WINDOW) < 0;
        return new ActivityItemDTO(justCreated ? "LISTING_CREATED" : "LISTING_UPDATED", id, title, agent, status,
                justCreated ? createdAt : updatedAt);
    }

    private static long number(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static Long agentScope() {
        List<Roles> roles = UserContext.getRoles();
        boolean admin = roles != null && (roles.contains(Roles.ADMIN) || roles.contains(Roles.MANAGER));
        return admin ? null : UserContext.getUserId();
    }
}
