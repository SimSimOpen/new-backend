package net.jemsit.product.service;

import lombok.RequiredArgsConstructor;
import net.jemsit.common.UserContext;
import net.jemsit.common.api.AuthApi;
import net.jemsit.common.api.OperatorFeedApi;
import net.jemsit.common.dto.response.auth.ProfileResponseDTO;
import net.jemsit.common.dto.response.auth.UserDetailsResponseDTO;
import net.jemsit.common.exceptions.UserException;
import net.jemsit.product.data.model.engagement.ListingKind;
import net.jemsit.product.data.model.report.ListingReport;
import net.jemsit.product.data.model.report.ReportReason;
import net.jemsit.product.data.model.report.ReportStatus;
import net.jemsit.product.dto.report.ListingReportDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Complaints about listings. A signed-in user files one (at most one open complaint per user per
 * listing); admins list them and close them as resolved or dismissed. Every new complaint is also a
 * card on the operator's phone.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    /** Deleted listings stay in, so a complaint about a listing that was taken down still reads well. */
    private static final String LISTINGS = Arrays.stream(ListingKind.values())
            .map(kind -> "SELECT '" + kind.name() + "' AS kind, id, title, agent, listing_status, is_deleted FROM "
                    + kind.table())
            .collect(Collectors.joining(" UNION ALL "));

    private static final String SELECT = """
            SELECT r.id, r.kind, r.listing_id, r.reporter_id, r.reason, r.details, r.status,
                   r.created_at, r.resolved_at,
                   l.title AS listing_title, l.agent AS listing_agent,
                   l.listing_status, l.is_deleted
            FROM listing_reports r
            LEFT JOIN (""" + LISTINGS + """
            ) l ON l.kind = r.kind AND l.id = r.listing_id
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final AuthApi authApi;
    private final OperatorFeedApi operatorFeedApi;

    /** Files the caller's complaint about a live listing and tells the operator. */
    @Transactional
    public ListingReportDTO create(ListingKind kind, Long listingId, ReportReason reason, String details) {
        Long me = UserContext.getUserId();
        if (me == null) {
            throw new UserException("Sign in to report a listing");
        }
        List<Map<String, Object>> listing = jdbc.queryForList("SELECT title, agentid FROM " + kind.table()
                        + " WHERE id = :id AND listing_status = 'ACTIVE' AND is_deleted = false",
                new MapSqlParameterSource("id", listingId));
        if (listing.isEmpty()) {
            throw new UserException("This listing is not available");
        }
        Object owner = listing.getFirst().get("agentid");
        if (owner instanceof Number n && n.longValue() == me) {
            throw new UserException("You cannot report your own listing");
        }
        String text = details == null ? null : details.strip();
        if (text != null && text.isEmpty()) {
            text = null;
        }
        if (text != null && text.length() > ListingReport.MAX_DETAILS) {
            throw new UserException("Details can be at most " + ListingReport.MAX_DETAILS + " characters long");
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("kind", kind.name())
                .addValue("listingId", listingId)
                .addValue("me", me)
                .addValue("reason", reason.name())
                .addValue("details", text)
                .addValue("open", ReportStatus.OPEN.name());
        Long existing = jdbc.queryForObject("""
                SELECT COUNT(*) FROM listing_reports
                WHERE kind = :kind AND listing_id = :listingId AND reporter_id = :me AND status = :open""",
                params, Long.class);
        if (existing != null && existing > 0) {
            throw new UserException("You have already reported this listing. We are looking into it.");
        }
        Long id = jdbc.queryForObject("""
                INSERT INTO listing_reports (kind, listing_id, reporter_id, reason, details, status, created_at)
                VALUES (:kind, :listingId, :me, :reason, :details, :open, now())
                RETURNING id""", params, Long.class);
        String reporter = displayName(me);
        operatorFeedApi.listingReported(id, kind.name(), listingId, (String) listing.getFirst().get("title"),
                reason.name(), text, reporter);
        Map<Long, String> names = new HashMap<>();
        names.put(me, reporter == null ? "" : reporter);
        return get(id, names);
    }

    /** Admin list, newest first; every filter is optional. */
    @Transactional(readOnly = true)
    public Page<ListingReportDTO> list(ReportStatus status, ListingKind kind, Long listingId, Pageable pageable) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (status != null) {
            where.append(" AND r.status = :status");
            params.addValue("status", status.name());
        }
        if (kind != null) {
            where.append(" AND r.kind = :kind");
            params.addValue("kind", kind.name());
        }
        if (listingId != null) {
            where.append(" AND r.listing_id = :listingId");
            params.addValue("listingId", listingId);
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM listing_reports r" + where, params, Long.class);
        params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        Map<Long, String> names = new HashMap<>();
        List<ListingReportDTO> rows = jdbc.query(SELECT + where + " ORDER BY r.created_at DESC, r.id DESC"
                        + " LIMIT :limit OFFSET :offset",
                params, (rs, i) -> toDto(rs, names));
        return new PageImpl<>(rows, pageable, total == null ? 0 : total);
    }

    /** Open complaints, for the admin menu badge. */
    @Transactional(readOnly = true)
    public long openCount() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM listing_reports WHERE status = :open",
                new MapSqlParameterSource("open", ReportStatus.OPEN.name()), Long.class);
        return count == null ? 0 : count;
    }

    /** Moves a complaint to any status; back to OPEN clears who closed it. */
    @Transactional
    public ListingReportDTO setStatus(Long id, ReportStatus status) {
        boolean open = status == ReportStatus.OPEN;
        int updated = jdbc.update("""
                UPDATE listing_reports
                SET status = :status,
                    resolved_at = CASE WHEN CAST(:open AS boolean) THEN NULL ELSE now() END,
                    resolved_by = CASE WHEN CAST(:open AS boolean) THEN NULL ELSE CAST(:me AS bigint) END
                WHERE id = :id""",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("status", status.name())
                        .addValue("open", open)
                        .addValue("me", UserContext.getUserId()));
        if (updated == 0) {
            throw new UserException("Complaint not found");
        }
        return get(id, new HashMap<>());
    }

    private ListingReportDTO get(Long id, Map<Long, String> names) {
        List<ListingReportDTO> found = jdbc.query(SELECT + " WHERE r.id = :id",
                new MapSqlParameterSource("id", id), (rs, i) -> toDto(rs, names));
        if (found.isEmpty()) {
            throw new UserException("Complaint not found");
        }
        return found.getFirst();
    }

    private ListingReportDTO toDto(java.sql.ResultSet rs, Map<Long, String> names) throws java.sql.SQLException {
        long reporterId = rs.getLong("reporter_id");
        String reporter = names.computeIfAbsent(reporterId, id -> {
            String name = displayName(id);
            return name == null ? "" : name;
        });
        String title = rs.getString("listing_title");
        Object deleted = rs.getObject("is_deleted");
        return new ListingReportDTO(
                rs.getLong("id"),
                ListingKind.valueOf(rs.getString("kind")),
                rs.getLong("listing_id"),
                title,
                rs.getString("listing_status"),
                title == null || Boolean.TRUE.equals(deleted),
                rs.getString("listing_agent"),
                ReportReason.valueOf(rs.getString("reason")),
                rs.getString("details"),
                ReportStatus.valueOf(rs.getString("status")),
                reporterId,
                reporter.isEmpty() ? null : reporter,
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("resolved_at")));
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

    /** Full name from the profile when there is one, otherwise the username (a phone number). */
    private String displayName(Long userId) {
        try {
            UserDetailsResponseDTO user = authApi.getById(userId);
            ProfileResponseDTO profile = user.profile();
            if (profile != null) {
                String name = String.join(" ",
                        profile.firstName() == null ? "" : profile.firstName(),
                        profile.lastName() == null ? "" : profile.lastName()).strip();
                if (!name.isEmpty()) {
                    return name;
                }
            }
            return user.username();
        } catch (UserException e) {
            return null;
        }
    }
}
