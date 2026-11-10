package net.jemsit.product.data.repository.booking;

import net.jemsit.product.data.model.booking.Booking;
import net.jemsit.product.data.model.booking.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Bookings of a listing in the given statuses that overlap [from, to). */
    @Query("""
            select b from Booking b
            where b.listingId = :listingId and b.status in :statuses
              and b.startAt < :to and b.endAt > :from
            """)
    List<Booking> findOverlapping(@Param("listingId") Long listingId,
                                  @Param("statuses") Collection<BookingStatus> statuses,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);

    boolean existsByListingIdAndClientIdAndStartAtAndStatusIn(Long listingId, Long clientId, LocalDateTime startAt,
                                                              Collection<BookingStatus> statuses);

    Page<Booking> findByClientIdOrderByStartAtDesc(Long clientId, Pageable pageable);

    Page<Booking> findByAgentIdAndStatusInAndEndAtAfterOrderByStartAtAsc(Long agentId,
                                                                         Collection<BookingStatus> statuses,
                                                                         LocalDateTime after, Pageable pageable);

    Page<Booking> findByAgentIdAndStatusInOrderByStartAtDesc(Long agentId, Collection<BookingStatus> statuses,
                                                             Pageable pageable);

    long countByAgentIdAndStatusAndEndAtAfter(Long agentId, BookingStatus status, LocalDateTime after);
}
