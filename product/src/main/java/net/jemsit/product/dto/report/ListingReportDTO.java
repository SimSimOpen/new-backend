package net.jemsit.product.dto.report;

import net.jemsit.product.data.model.engagement.ListingKind;
import net.jemsit.product.data.model.report.ReportReason;
import net.jemsit.product.data.model.report.ReportStatus;

import java.time.Instant;

/**
 * One complaint as the admin panel shows it. The listing fields are null when the listing no longer
 * exists; {@code listingDeleted} tells a deleted listing from a live one.
 */
public record ListingReportDTO(Long id,
                               ListingKind kind,
                               Long listingId,
                               String listingTitle,
                               String listingStatus,
                               boolean listingDeleted,
                               String agent,
                               ReportReason reason,
                               String details,
                               ReportStatus status,
                               Long reporterId,
                               String reporter,
                               Instant createdAt,
                               Instant resolvedAt) {
}
