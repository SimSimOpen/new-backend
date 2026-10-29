package net.jemsit.common.data.enums.service;

/**
 * How the number in {@code price} is meant. Services are quoted per job or per hour, so the amount
 * on its own would be misleading; NEGOTIABLE lets a poster show no figure at all, which is also how
 * a seeker says "make me an offer".
 */
public enum PriceMode {
    FIXED,
    HOURLY,
    NEGOTIABLE
}
