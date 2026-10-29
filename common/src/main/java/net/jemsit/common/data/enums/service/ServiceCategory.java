package net.jemsit.common.data.enums.service;

/**
 * What a services listing is about. One taxonomy for both sides of the market: a plumber offering
 * repairs and someone looking for a plumber both file under REPAIRS_CONSTRUCTION, which is what lets
 * the client's intent switch show the matching side without a second category list.
 */
public enum ServiceCategory {
    REPAIRS_CONSTRUCTION,
    CLEANING,
    TUTORING,
    MOVING_DELIVERY,
    BEAUTY_HEALTH,
    IT_DIGITAL,
    AUTO,
    EVENTS_CATERING,
    HOUSEHOLD,
    OTHER
}
