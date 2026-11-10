package net.jemsit.product.data.repository.booking;

import jakarta.persistence.LockModeType;
import net.jemsit.product.data.model.booking.ServiceSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ServiceScheduleRepository extends JpaRepository<ServiceSchedule, Long> {

    Optional<ServiceSchedule> findByListingId(Long listingId);

    /** Taken while accepting, so two accepts for the same listing run one after the other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ServiceSchedule s where s.listingId = :listingId")
    Optional<ServiceSchedule> lockByListingId(@Param("listingId") Long listingId);
}
