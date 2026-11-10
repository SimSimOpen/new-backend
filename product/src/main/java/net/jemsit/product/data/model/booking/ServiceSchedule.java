package net.jemsit.product.data.model.booking;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import net.jemsit.product.data.model.BaseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * When a service listing (a barber shop, say) takes appointments. Times are wall-clock times in
 * {@code BookingService.ZONE}; a listing without a schedule, or with {@code enabled = false}, is not bookable.
 */
@Entity
@Table(name = "service_schedules")
@Getter
@Setter
public class ServiceSchedule extends BaseEntity {
    @Column(name = "listing_id", nullable = false, unique = true)
    private Long listingId;
    @Column(name = "agent_id", nullable = false)
    private Long agentId;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(name = "slot_minutes", nullable = false)
    private int slotMinutes = 30;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "service_schedule_hours", joinColumns = @JoinColumn(name = "schedule_id"))
    private List<WorkingHours> hours = new ArrayList<>();
}
