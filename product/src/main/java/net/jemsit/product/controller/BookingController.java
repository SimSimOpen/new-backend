package net.jemsit.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.jemsit.product.dto.booking.BookingDTOs.BookingDTO;
import net.jemsit.product.dto.booking.BookingDTOs.BookingRequestDTO;
import net.jemsit.product.dto.booking.BookingDTOs.DaySlotsDTO;
import net.jemsit.product.dto.booking.BookingDTOs.ScheduleDTO;
import net.jemsit.product.service.BookingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/** Appointments for service listings. schedule/{id} (GET) and slots/{id} are public. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/product/v1/booking")
public class BookingController {

    private final BookingService bookingService;

    @GetMapping("schedule/{listingId}")
    public ScheduleDTO getSchedule(@PathVariable Long listingId) {
        return bookingService.getSchedule(listingId);
    }

    @PutMapping("schedule/{listingId}")
    public ScheduleDTO saveSchedule(@PathVariable Long listingId, @Valid @RequestBody ScheduleDTO request) {
        return bookingService.saveSchedule(listingId, request);
    }

    @GetMapping("slots/{listingId}")
    public DaySlotsDTO getSlots(@PathVariable Long listingId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.getSlots(listingId, date);
    }

    @PostMapping("request")
    public BookingDTO request(@Valid @RequestBody BookingRequestDTO request) {
        return bookingService.request(request);
    }

    @GetMapping("mine")
    public Page<BookingDTO> mine(Pageable pageable) {
        return bookingService.mine(pageable);
    }

    @PostMapping("{id}/cancel")
    public BookingDTO cancel(@PathVariable Long id) {
        return bookingService.cancel(id);
    }

    @GetMapping("agent")
    public Page<BookingDTO> forAgent(@RequestParam(defaultValue = "upcoming") String view, Pageable pageable) {
        return bookingService.forAgent(view, pageable);
    }

    @GetMapping("agent/pending-count")
    public Map<String, Long> pendingCount() {
        return Map.of("count", bookingService.pendingCount());
    }

    @PostMapping("{id}/accept")
    public BookingDTO accept(@PathVariable Long id) {
        return bookingService.accept(id);
    }

    @PostMapping("{id}/reject")
    public BookingDTO reject(@PathVariable Long id) {
        return bookingService.reject(id);
    }
}
