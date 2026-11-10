package net.jemsit.product.dto.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.jemsit.product.data.model.booking.BookingStatus;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Request and response shapes of the booking API; times are wall-clock times in Asia/Tashkent. */
public final class BookingDTOs {
    private BookingDTOs() {
    }

    public record WorkingHoursDTO(@NotNull DayOfWeek dayOfWeek, @NotNull LocalTime openTime,
                                  @NotNull LocalTime closeTime) {
    }

    public record ScheduleDTO(Long listingId, boolean enabled,
                              @Min(5) @Max(480) int slotMinutes,
                              @NotNull @Size(max = 28) List<@Valid WorkingHoursDTO> hours) {
    }

    public enum SlotState { FREE, TAKEN }

    public record SlotDTO(LocalDateTime startAt, LocalDateTime endAt, SlotState state) {
    }

    public record DaySlotsDTO(Long listingId, boolean bookable, int slotMinutes, List<SlotDTO> slots) {
    }

    public record BookingRequestDTO(@NotNull Long listingId, @NotNull LocalDateTime startAt,
                                    @Size(max = 500) String note) {
    }

    public record BookingDTO(Long id, Long listingId, String listingTitle, Long agentId, Long clientId,
                             String clientName, String clientPhone, LocalDateTime startAt, LocalDateTime endAt,
                             BookingStatus status, String note, LocalDateTime createdAt) {
    }

    /** Pushed over SSE as a {@code booking} event so open apps refresh without polling. */
    public record BookingEventPayload(String type, BookingDTO booking) {
    }
}
