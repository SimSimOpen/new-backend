package net.jemsit.product.data.model.engagement;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** A listing shared by a browser, counted at most once per day. Written with SQL in EngagementService. */
@Entity
@Table(name = "listing_shares",
        uniqueConstraints = @UniqueConstraint(columnNames = {"kind", "listing_id", "visitor_id", "shared_on"}))
@Getter
@Setter
public class ListingShare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String kind;
    @Column(name = "listing_id", nullable = false)
    private Long listingId;
    @Column(name = "visitor_id", nullable = false)
    private String visitorId;
    @Column(name = "shared_on", nullable = false)
    private LocalDate sharedOn;
}
