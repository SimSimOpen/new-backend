package net.jemsit.product.data.model.booking;

/** PENDING until the agent answers; CANCELLED is the client withdrawing (pending or accepted). */
public enum BookingStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED
}
