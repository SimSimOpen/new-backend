package net.jemsit.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.common.UserContext;
import net.jemsit.common.api.AuthApi;
import net.jemsit.common.data.enums.Roles;
import net.jemsit.common.data.enums.property.ListingStatus;
import net.jemsit.common.dto.message.BookingEvent;
import net.jemsit.common.dto.response.auth.UserDetailsResponseDTO;
import net.jemsit.common.exceptions.UserException;
import net.jemsit.product.data.model.booking.Booking;
import net.jemsit.product.data.model.booking.BookingStatus;
import net.jemsit.product.data.model.booking.ServiceSchedule;
import net.jemsit.product.data.model.booking.WorkingHours;
import net.jemsit.product.data.model.services.ServiceListing;
import net.jemsit.product.data.repository.booking.BookingRepository;
import net.jemsit.product.data.repository.booking.ServiceScheduleRepository;
import net.jemsit.product.data.repository.services.ServiceListingRepository;
import net.jemsit.product.dto.booking.BookingDTOs.BookingDTO;
import net.jemsit.product.dto.booking.BookingDTOs.BookingEventPayload;
import net.jemsit.product.dto.booking.BookingDTOs.BookingRequestDTO;
import net.jemsit.product.dto.booking.BookingDTOs.DaySlotsDTO;
import net.jemsit.product.dto.booking.BookingDTOs.ScheduleDTO;
import net.jemsit.product.dto.booking.BookingDTOs.SlotDTO;
import net.jemsit.product.dto.booking.BookingDTOs.SlotState;
import net.jemsit.product.dto.booking.BookingDTOs.WorkingHoursDTO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Appointments for service listings: the agent publishes weekly working hours split into equal
 * slots, a client requests a slot, and the agent accepts or rejects it. A slot is taken once a
 * booking for it is ACCEPTED; several clients may have a PENDING request for the same free slot,
 * and accepting one rejects the others.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    /** The marketplace is in Uzbekistan; all schedule and booking times are wall-clock times here. */
    public static final ZoneId ZONE = ZoneId.of("Asia/Tashkent");
    /** How far ahead a client may book. */
    private static final int MAX_DAYS_AHEAD = 60;

    private static final Set<BookingStatus> OPEN = EnumSet.of(BookingStatus.PENDING, BookingStatus.ACCEPTED);
    private static final Set<BookingStatus> ACCEPTED = EnumSet.of(BookingStatus.ACCEPTED);
    private static final Set<BookingStatus> CLOSED =
            EnumSet.of(BookingStatus.REJECTED, BookingStatus.CANCELLED);

    private final ServiceScheduleRepository scheduleRepository;
    private final BookingRepository bookingRepository;
    private final ServiceListingRepository listingRepository;
    private final AuthApi authApi;
    private final ApplicationEventPublisher eventPublisher;

    // ---------------------------------------------------------------- schedule

    /** Public. A listing that never had a schedule reads as a disabled, empty one. */
    @Transactional(readOnly = true)
    public ScheduleDTO getSchedule(Long listingId) {
        return scheduleRepository.findByListingId(listingId)
                .map(BookingService::toDto)
                .orElse(new ScheduleDTO(listingId, false, 30, List.of()));
    }

    @Transactional
    public ScheduleDTO saveSchedule(Long listingId, ScheduleDTO request) {
        ServiceListing listing = requireOwnedListing(listingId);
        List<WorkingHours> hours = new ArrayList<>();
        for (WorkingHoursDTO h : request.hours()) {
            if (!h.closeTime().isAfter(h.openTime())) {
                throw new UserException("Closing time must be after opening time on " + h.dayOfWeek());
            }
            hours.add(new WorkingHours(h.dayOfWeek(), h.openTime(), h.closeTime()));
        }
        hours.sort(Comparator.comparing(WorkingHours::getDayOfWeek).thenComparing(WorkingHours::getOpenTime));
        for (int i = 1; i < hours.size(); i++) {
            WorkingHours prev = hours.get(i - 1);
            WorkingHours cur = hours.get(i);
            if (prev.getDayOfWeek() == cur.getDayOfWeek() && cur.getOpenTime().isBefore(prev.getCloseTime())) {
                throw new UserException("Working hours overlap on " + cur.getDayOfWeek());
            }
        }
        ServiceSchedule schedule = scheduleRepository.findByListingId(listingId).orElseGet(ServiceSchedule::new);
        schedule.setListingId(listingId);
        schedule.setAgentId(listing.getAgentID());
        schedule.setEnabled(request.enabled());
        schedule.setSlotMinutes(request.slotMinutes());
        schedule.getHours().clear();
        schedule.getHours().addAll(hours);
        return toDto(scheduleRepository.save(schedule));
    }

    // ---------------------------------------------------------------- slots

    /** Public: the slots of one day, past ones left out, accepted ones marked TAKEN. */
    @Transactional(readOnly = true)
    public DaySlotsDTO getSlots(Long listingId, LocalDate date) {
        ServiceSchedule schedule = scheduleRepository.findByListingId(listingId).orElse(null);
        if (schedule == null || !schedule.isEnabled() || !isLive(listingId)) {
            return new DaySlotsDTO(listingId, false, schedule == null ? 30 : schedule.getSlotMinutes(), List.of());
        }
        LocalDate today = LocalDate.now(ZONE);
        if (date.isBefore(today) || date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            return new DaySlotsDTO(listingId, true, schedule.getSlotMinutes(), List.of());
        }
        LocalDateTime dayStart = date.atStartOfDay();
        List<Booking> taken = bookingRepository.findOverlapping(listingId, ACCEPTED, dayStart, dayStart.plusDays(1));
        LocalDateTime now = LocalDateTime.now(ZONE);

        List<SlotDTO> slots = new ArrayList<>();
        for (LocalDateTime[] slot : daySlots(schedule, date)) {
            if (!slot[0].isAfter(now)) {
                continue;
            }
            boolean busy = taken.stream().anyMatch(b -> b.getStartAt().isBefore(slot[1]) && b.getEndAt().isAfter(slot[0]));
            slots.add(new SlotDTO(slot[0], slot[1], busy ? SlotState.TAKEN : SlotState.FREE));
        }
        return new DaySlotsDTO(listingId, true, schedule.getSlotMinutes(), slots);
    }

    /** Every slot the schedule defines on that date, as [start, end) pairs. */
    private static List<LocalDateTime[]> daySlots(ServiceSchedule schedule, LocalDate date) {
        List<LocalDateTime[]> result = new ArrayList<>();
        int step = schedule.getSlotMinutes();
        for (WorkingHours h : schedule.getHours()) {
            if (h.getDayOfWeek() != date.getDayOfWeek()) {
                continue;
            }
            LocalDateTime close = date.atTime(h.getCloseTime());
            for (LocalDateTime start = date.atTime(h.getOpenTime());
                 !start.plusMinutes(step).isAfter(close);
                 start = start.plusMinutes(step)) {
                result.add(new LocalDateTime[]{start, start.plusMinutes(step)});
            }
        }
        result.sort(Comparator.comparing(s -> s[0]));
        return result;
    }

    // ---------------------------------------------------------------- client side

    @Transactional
    public BookingDTO request(BookingRequestDTO request) {
        Long clientId = UserContext.getUserId();
        ServiceListing listing = listingRepository.findById(request.listingId())
                .filter(l -> l.getListingStatus() == ListingStatus.ACTIVE && !Boolean.TRUE.equals(l.getIsDeleted()))
                .orElseThrow(() -> new UserException("Service listing not found"));
        if (Objects.equals(listing.getAgentID(), clientId)) {
            throw new UserException("You cannot book your own service");
        }
        ServiceSchedule schedule = scheduleRepository.findByListingId(listing.getId())
                .filter(ServiceSchedule::isEnabled)
                .orElseThrow(() -> new UserException("This service does not take bookings"));

        LocalDateTime start = request.startAt().withSecond(0).withNano(0);
        LocalDateTime[] slot = daySlots(schedule, start.toLocalDate()).stream()
                .filter(s -> s[0].equals(start))
                .findFirst()
                .orElseThrow(() -> new UserException("This time is outside the working hours"));
        LocalDate today = LocalDate.now(ZONE);
        if (!start.isAfter(LocalDateTime.now(ZONE)) || start.toLocalDate().isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw new UserException("This time can no longer be booked");
        }
        if (!bookingRepository.findOverlapping(listing.getId(), ACCEPTED, slot[0], slot[1]).isEmpty()) {
            throw new UserException("This time is already taken");
        }
        if (bookingRepository.existsByListingIdAndClientIdAndStartAtAndStatusIn(listing.getId(), clientId, start, OPEN)) {
            throw new UserException("You have already requested this time");
        }

        Booking booking = new Booking();
        booking.setListingId(listing.getId());
        booking.setListingTitle(listing.getTitle());
        booking.setAgentId(listing.getAgentID());
        booking.setClientId(clientId);
        fillClient(booking, clientId);
        booking.setStartAt(slot[0]);
        booking.setEndAt(slot[1]);
        booking.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
        BookingDTO dto = toDto(bookingRepository.save(booking));
        publish("REQUESTED", dto);
        return dto;
    }

    @Transactional(readOnly = true)
    public Page<BookingDTO> mine(Pageable pageable) {
        return bookingRepository.findByClientIdOrderByStartAtDesc(UserContext.getUserId(), pageable)
                .map(BookingService::toDto);
    }

    /** The client withdraws a request, or an accepted booking that has not started yet. */
    @Transactional
    public BookingDTO cancel(Long id) {
        Booking booking = requireBooking(id);
        if (!Objects.equals(booking.getClientId(), UserContext.getUserId())) {
            throw new UserException("Booking not found");
        }
        if (!OPEN.contains(booking.getStatus()) || !booking.getStartAt().isAfter(LocalDateTime.now(ZONE))) {
            throw new UserException("This booking can no longer be cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        BookingDTO dto = toDto(booking);
        publish("CANCELLED", dto);
        return dto;
    }

    // ---------------------------------------------------------------- agent side

    /**
     * The agent's bookings. {@code upcoming} (default) is pending + accepted ones not yet over,
     * soonest first; {@code history} is everything else, newest first.
     */
    @Transactional(readOnly = true)
    public Page<BookingDTO> forAgent(String view, Pageable pageable) {
        Long agentId = UserContext.getUserId();
        if ("history".equalsIgnoreCase(view)) {
            return bookingRepository.findByAgentIdAndStatusInOrderByStartAtDesc(agentId, EnumSet.allOf(BookingStatus.class), pageable)
                    .map(BookingService::toDto);
        }
        return bookingRepository.findByAgentIdAndStatusInAndEndAtAfterOrderByStartAtAsc(
                agentId, OPEN, LocalDateTime.now(ZONE), pageable).map(BookingService::toDto);
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return bookingRepository.countByAgentIdAndStatusAndEndAtAfter(
                UserContext.getUserId(), BookingStatus.PENDING, LocalDateTime.now(ZONE));
    }

    @Transactional
    public BookingDTO accept(Long id) {
        Booking booking = requireAgentBooking(id);
        // Serializes accepts per listing: the overlap check below cannot race another accept.
        scheduleRepository.lockByListingId(booking.getListingId());
        requirePending(booking);
        if (!booking.getStartAt().isAfter(LocalDateTime.now(ZONE))) {
            throw new UserException("This time has already passed");
        }
        List<Booking> overlapping = bookingRepository.findOverlapping(
                booking.getListingId(), OPEN, booking.getStartAt(), booking.getEndAt());
        if (overlapping.stream().anyMatch(b -> b.getStatus() == BookingStatus.ACCEPTED)) {
            throw new UserException("Another booking is already accepted for this time");
        }
        booking.setStatus(BookingStatus.ACCEPTED);
        BookingDTO dto = toDto(booking);
        publish("ACCEPTED", dto);

        // The slot is gone: the other clients who asked for it are told straight away.
        for (Booking other : overlapping) {
            if (!other.getId().equals(booking.getId()) && other.getStatus() == BookingStatus.PENDING) {
                other.setStatus(BookingStatus.REJECTED);
                publish("REJECTED", toDto(other));
            }
        }
        return dto;
    }

    @Transactional
    public BookingDTO reject(Long id) {
        Booking booking = requireAgentBooking(id);
        if (booking.getStatus() == BookingStatus.ACCEPTED) {
            // An accepted booking may still be called off by the shop (sick day etc).
            if (!booking.getStartAt().isAfter(LocalDateTime.now(ZONE))) {
                throw new UserException("This booking has already started");
            }
        } else {
            requirePending(booking);
        }
        booking.setStatus(BookingStatus.REJECTED);
        BookingDTO dto = toDto(booking);
        publish("REJECTED", dto);
        return dto;
    }

    // ---------------------------------------------------------------- helpers

    private boolean isLive(Long listingId) {
        return listingRepository.findById(listingId)
                .map(l -> l.getListingStatus() == ListingStatus.ACTIVE && !Boolean.TRUE.equals(l.getIsDeleted()))
                .orElse(false);
    }

    private ServiceListing requireOwnedListing(Long listingId) {
        ServiceListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new UserException("Service listing not found"));
        if (!hasRole(Roles.ADMIN) && !Objects.equals(listing.getAgentID(), UserContext.getUserId())) {
            throw new UserException("You are not allowed to change this listing's schedule");
        }
        return listing;
    }

    private Booking requireBooking(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new UserException("Booking not found"));
    }

    private Booking requireAgentBooking(Long id) {
        Booking booking = requireBooking(id);
        if (!hasRole(Roles.ADMIN) && !Objects.equals(booking.getAgentId(), UserContext.getUserId())) {
            throw new UserException("Booking not found");
        }
        return booking;
    }

    private static void requirePending(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new UserException("This booking was already " + booking.getStatus().name().toLowerCase());
        }
    }

    /** Name and phone are copied in so the agent's list needs no per-row lookups. */
    private void fillClient(Booking booking, Long clientId) {
        try {
            UserDetailsResponseDTO user = authApi.getById(clientId);
            var profile = user.profile();
            String name = profile == null ? null
                    : ((Objects.toString(profile.firstName(), "") + " " + Objects.toString(profile.lastName(), "")).trim());
            booking.setClientName(name == null || name.isBlank() ? user.username() : name);
            // OTP accounts are keyed by their phone number, so the username is the fallback; a
            // username that is not a number (email sign-ups) is no use to call.
            String phone = profile == null ? null : profile.phoneNumber();
            if (phone == null || phone.isBlank()) {
                phone = user.username();
            }
            booking.setClientPhone(phone != null && phone.matches("^\\+?[0-9 ()-]{7,20}$") ? phone : null);
        } catch (RuntimeException e) {
            log.warn("Client {} details could not be loaded for a booking: {}", clientId, e.getMessage());
        }
    }

    private void publish(String type, BookingDTO dto) {
        eventPublisher.publishEvent(new BookingEvent(List.of(dto.agentId(), dto.clientId()),
                new BookingEventPayload(type, dto)));
    }

    private static boolean hasRole(Roles role) {
        var roles = UserContext.getRoles();
        return roles != null && roles.contains(role);
    }

    private static ScheduleDTO toDto(ServiceSchedule s) {
        return new ScheduleDTO(s.getListingId(), s.isEnabled(), s.getSlotMinutes(),
                s.getHours().stream()
                        .map(h -> new WorkingHoursDTO(h.getDayOfWeek(), h.getOpenTime(), h.getCloseTime()))
                        .toList());
    }

    private static BookingDTO toDto(Booking b) {
        return new BookingDTO(b.getId(), b.getListingId(), b.getListingTitle(), b.getAgentId(), b.getClientId(),
                b.getClientName(), b.getClientPhone(), b.getStartAt(), b.getEndAt(), b.getStatus(), b.getNote(),
                b.getCreatedAt());
    }
}
