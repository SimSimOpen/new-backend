package net.jemsit.product.data.model.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A user's complaint about a listing. Rows are written with SQL in ReportService; this defines the
 * table. {@code kind} plus {@code listing_id} name the listing, as in chats and saves.
 */
@Entity
@Table(name = "listing_reports",
        indexes = {
                @Index(columnList = "status, created_at"),
                @Index(columnList = "kind, listing_id"),
                @Index(columnList = "reporter_id")
        })
@Getter
@Setter
public class ListingReport {
    public static final int MAX_DETAILS = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 32)
    private String kind;
    @Column(name = "listing_id", nullable = false)
    private Long listingId;
    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;
    @Column(nullable = false, length = 32)
    private String reason;
    @Column(length = MAX_DETAILS)
    private String details;
    @Column(nullable = false, length = 16)
    private String status;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    @Column(name = "resolved_by")
    private Long resolvedBy;
}
