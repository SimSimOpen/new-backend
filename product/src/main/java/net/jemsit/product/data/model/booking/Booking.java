package net.jemsit.product.data.model.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import net.jemsit.product.data.model.BaseEntity;

import java.time.LocalDateTime;

/** A client's request for one slot of a service listing. Title and names are snapshots for the lists. */
@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_listing_start", columnList = "listing_id, start_at"),
        @Index(name = "idx_booking_agent", columnList = "agent_id, start_at"),
        @Index(name = "idx_booking_client", columnList = "client_id, start_at")
})
@Getter
@Setter
public class Booking extends BaseEntity {
    @Column(name = "listing_id", nullable = false)
    private Long listingId;
    @Column(name = "listing_title")
    private String listingTitle;
    @Column(name = "agent_id", nullable = false)
    private Long agentId;
    @Column(name = "client_id", nullable = false)
    private Long clientId;
    @Column(name = "client_name")
    private String clientName;
    @Column(name = "client_phone")
    private String clientPhone;
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;
    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING;
    @Column(length = 500)
    private String note;
}
