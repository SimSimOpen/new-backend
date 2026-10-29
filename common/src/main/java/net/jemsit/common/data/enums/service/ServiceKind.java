package net.jemsit.common.data.enums.service;

/**
 * Which side of the market a listing was posted from. The services category is two-sided: a supplier
 * publishes what they can do, and someone who needs a job done publishes that they are looking for
 * it. Each side browses the other one, so the client asks for a kind and the poster picks one.
 *
 * <ul>
 *   <li>{@code OFFERING} - the poster supplies the service and is looking for customers.</li>
 *   <li>{@code SEEKING} - the poster needs the service and is looking for a supplier; {@code price}
 *       reads as their budget.</li>
 * </ul>
 */
public enum ServiceKind {
    OFFERING,
    SEEKING
}
