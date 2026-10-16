package net.jemsit.notification.presence;

/**
 * @param siteVisitors  distinct browsers with the website open, signed in or not
 * @param siteSignedIn  of those, how many are signed in (counted by account, so two tabs are one)
 * @param adminOnline   staff with the admin panel open
 * @param windowSeconds how recently someone must have been seen to count
 */
public record PresenceStatsDTO(long siteVisitors, long siteSignedIn, long adminOnline, long windowSeconds) {
}
