package net.jemsit.product.data.model.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** One open interval on one weekday; a day without a row is closed. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkingHours {
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;
    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;
    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;
}
