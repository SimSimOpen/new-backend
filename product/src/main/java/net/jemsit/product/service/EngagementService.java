package net.jemsit.product.service;

import lombok.RequiredArgsConstructor;
import net.jemsit.common.UserContext;
import net.jemsit.common.data.enums.Roles;
import net.jemsit.common.dto.response.dashboard.MonthlyCountDTO;
import net.jemsit.common.exceptions.UserException;
import net.jemsit.product.data.model.engagement.ListingKind;
import net.jemsit.product.dto.engagement.EngagementStatsDTO;
import net.jemsit.product.dto.engagement.SavedListingDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Saves and shares of public listings, and the statistics built from them. Visitors need no account:
 * they are identified by the random id their browser keeps, the one presence heartbeats already use.
 */
@Service
@RequiredArgsConstructor
public class EngagementService {

    private static final Pattern VISITOR_ID = Pattern.compile("^[0-9a-fA-F-]{36}$");
    private static final int MONTHS = 12;
    private static final int TOP_LISTINGS = 50;
    private static final Map<String, String> SORT_COLUMNS = Map.of("views", "views", "saves", "saves", "shares", "shares");

    private static final String LISTINGS = Arrays.stream(ListingKind.values())
            .map(kind -> "SELECT '" + kind.name() + "' AS kind, id, title, agent, agentid, view_count, listing_status FROM "
                    + kind.table() + " WHERE is_deleted = false")
            .collect(Collectors.joining(" UNION ALL "));

    /** Listings in scope: optionally one kind, optionally only the caller's. */
    private static final String SCOPED = "WITH scoped AS (SELECT * FROM (" + LISTINGS + ") l "
            + "WHERE (CAST(:kind AS text) IS NULL OR l.kind = :kind) "
            + "AND (CAST(:agentId AS bigint) IS NULL OR l.agentid = :agentId)) ";

    private final NamedParameterJdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public List<SavedListingDTO> getSaved(String visitorId) {
        return jdbc.query("""
                SELECT kind, listing_id FROM listing_saves WHERE visitor_id = :visitorId ORDER BY created_at DESC, id DESC""",
                new MapSqlParameterSource("visitorId", requireVisitor(visitorId)),
                (rs, i) -> new SavedListingDTO(ListingKind.valueOf(rs.getString("kind")), rs.getLong("listing_id")));
    }

    /** Idempotent. Only a live listing can be saved, so the table cannot fill with arbitrary ids. */
    @Transactional
    public void save(ListingKind kind, Long listingId, String visitorId) {
        jdbc.update("INSERT INTO listing_saves (kind, listing_id, visitor_id, created_at) "
                        + "SELECT :kind, :listingId, :visitorId, now() " + whereLive(kind)
                        + " ON CONFLICT (kind, listing_id, visitor_id) DO NOTHING",
                params(kind, listingId, requireVisitor(visitorId)));
    }

    @Transactional
    public void unsave(ListingKind kind, Long listingId, String visitorId) {
        jdbc.update("DELETE FROM listing_saves WHERE kind = :kind AND listing_id = :listingId AND visitor_id = :visitorId",
                params(kind, listingId, requireVisitor(visitorId)));
    }

    @Transactional
    public void recordShare(ListingKind kind, Long listingId, String visitorId) {
        jdbc.update("INSERT INTO listing_shares (kind, listing_id, visitor_id, shared_on) "
                        + "SELECT :kind, :listingId, :visitorId, current_date " + whereLive(kind)
                        + " ON CONFLICT (kind, listing_id, visitor_id, shared_on) DO NOTHING",
                params(kind, listingId, requireVisitor(visitorId)));
    }

    /** Agents see their own listings; admins and managers see everything. */
    @Transactional(readOnly = true)
    public EngagementStatsDTO getStats(ListingKind kind, String sort) {
        String sortColumn = SORT_COLUMNS.getOrDefault(sort, "views");
        var params = new MapSqlParameterSource()
                .addValue("kind", kind == null ? null : kind.name())
                .addValue("agentId", agentScope())
                .addValue("since", YearMonth.now().minusMonths(MONTHS - 1).atDay(1).atStartOfDay());

        Map<String, Object> totals = jdbc.queryForMap(SCOPED + """
                SELECT COUNT(*) AS listings,
                       COALESCE(SUM(view_count), 0) AS views,
                       (SELECT COUNT(*) FROM listing_saves x JOIN scoped s ON s.kind = x.kind AND s.id = x.listing_id) AS saves,
                       (SELECT COUNT(*) FROM listing_shares x JOIN scoped s ON s.kind = x.kind AND s.id = x.listing_id) AS shares
                FROM scoped""", params);

        List<EngagementStatsDTO.ListingRow> top = jdbc.query(SCOPED + """
                SELECT s.kind, s.id, s.title, s.agent, s.listing_status, s.view_count AS views,
                       COALESCE(sv.saves, 0) AS saves, COALESCE(sh.shares, 0) AS shares
                FROM scoped s
                LEFT JOIN (SELECT kind, listing_id, COUNT(*) AS saves FROM listing_saves GROUP BY 1, 2) sv
                       ON sv.kind = s.kind AND sv.listing_id = s.id
                LEFT JOIN (SELECT kind, listing_id, COUNT(*) AS shares FROM listing_shares GROUP BY 1, 2) sh
                       ON sh.kind = s.kind AND sh.listing_id = s.id
                ORDER BY""" + " " + sortColumn + " DESC, s.id DESC LIMIT " + TOP_LISTINGS, params,
                (rs, i) -> new EngagementStatsDTO.ListingRow(rs.getString("kind"), rs.getLong("id"), rs.getString("title"),
                        rs.getString("agent"), rs.getString("listing_status"), rs.getLong("views"), rs.getLong("saves"),
                        rs.getLong("shares")));

        return new EngagementStatsDTO(
                ((Number) totals.get("listings")).longValue(),
                ((Number) totals.get("views")).longValue(),
                ((Number) totals.get("saves")).longValue(),
                ((Number) totals.get("shares")).longValue(),
                perMonth("listing_saves", "created_at", params),
                perMonth("listing_shares", "shared_on", params),
                top);
    }

    private List<MonthlyCountDTO> perMonth(String table, String dateColumn, MapSqlParameterSource params) {
        Map<String, Long> counts = new HashMap<>();
        jdbc.query(SCOPED + "SELECT to_char(x." + dateColumn + ", 'YYYY-MM') AS month, COUNT(*) AS count FROM " + table
                        + " x JOIN scoped s ON s.kind = x.kind AND s.id = x.listing_id"
                        + " WHERE x." + dateColumn + " >= :since GROUP BY 1", params,
                rs -> {
                    counts.put(rs.getString("month"), rs.getLong("count"));
                });
        List<MonthlyCountDTO> months = new ArrayList<>();
        YearMonth month = YearMonth.now().minusMonths(MONTHS - 1);
        for (int i = 0; i < MONTHS; i++, month = month.plusMonths(1)) {
            months.add(new MonthlyCountDTO(month.toString(), counts.getOrDefault(month.toString(), 0L)));
        }
        return months;
    }

    private static String whereLive(ListingKind kind) {
        return "WHERE EXISTS (SELECT 1 FROM " + kind.table()
                + " WHERE id = :listingId AND listing_status = 'ACTIVE' AND is_deleted = false)";
    }

    private static MapSqlParameterSource params(ListingKind kind, Long listingId, String visitorId) {
        return new MapSqlParameterSource()
                .addValue("kind", kind.name())
                .addValue("listingId", listingId)
                .addValue("visitorId", visitorId);
    }

    private static String requireVisitor(String visitorId) {
        if (visitorId == null || !VISITOR_ID.matcher(visitorId).matches()) {
            throw new UserException("Visitor id is not valid");
        }
        return visitorId;
    }

    private static Long agentScope() {
        List<Roles> roles = UserContext.getRoles();
        boolean admin = roles != null && (roles.contains(Roles.ADMIN) || roles.contains(Roles.MANAGER));
        return admin ? null : UserContext.getUserId();
    }
}
