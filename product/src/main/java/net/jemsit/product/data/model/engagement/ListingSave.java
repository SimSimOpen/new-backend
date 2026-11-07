package net.jemsit.product.data.model.engagement;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** One browser's saved listing. Rows are written with SQL in EngagementService; this defines the table. */
@Entity
@Table(name = "listing_saves",
        uniqueConstraints = @UniqueConstraint(columnNames = {"kind", "listing_id", "visitor_id"}),
        indexes = @Index(columnList = "visitor_id"))
@Getter
@Setter
public class ListingSave {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String kind;
    @Column(name = "listing_id", nullable = false)
    private Long listingId;
    @Column(name = "visitor_id", nullable = false)
    private String visitorId;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
