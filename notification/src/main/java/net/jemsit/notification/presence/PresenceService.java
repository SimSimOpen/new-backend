package net.jemsit.notification.presence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Who has the website or the admin panel open right now. Each open page sends a heartbeat every
 * {@link #HEARTBEAT}; anyone heard from within {@link #ONLINE_WINDOW} counts as online. Last-seen
 * times live in Redis sorted sets (member = visitor/user id, score = epoch millis), so the numbers are
 * shared by every backend instance and old entries are trimmed on each write.
 */
@Service
@RequiredArgsConstructor
public class PresenceService {

    /** How often the frontends ping. Kept here so the window below is always a few beats wide. */
    public static final Duration HEARTBEAT = Duration.ofSeconds(30);
    /** Three missed beats: tolerant of a slow network, quick enough to drop closed tabs. */
    public static final Duration ONLINE_WINDOW = HEARTBEAT.multipliedBy(3);

    private static final String SITE_VISITORS = "presence:site:visitors";
    private static final String SITE_USERS = "presence:site:users";
    private static final String ADMIN_USERS = "presence:admin:users";
    /** Visitor ids are random UUIDs made by the browser; anything else is not one of ours. */
    private static final Pattern VISITOR_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");

    private final StringRedisTemplate redis;

    /**
     * Records a heartbeat. {@code userId} is null for visitors who are not signed in. Admin-panel
     * beats only count signed-in staff; the panel is useless without an account.
     */
    public void heartbeat(String app, String visitorId, Long userId) {
        if ("admin".equals(app)) {
            if (userId != null) {
                touch(ADMIN_USERS, userId.toString());
            }
            return;
        }
        if (visitorId == null || !VISITOR_ID.matcher(visitorId).matches()) {
            throw new IllegalArgumentException("Invalid visitor id");
        }
        touch(SITE_VISITORS, visitorId);
        if (userId != null) {
            touch(SITE_USERS, userId.toString());
        }
    }

    public PresenceStatsDTO stats() {
        return new PresenceStatsDTO(count(SITE_VISITORS), count(SITE_USERS), count(ADMIN_USERS),
                ONLINE_WINDOW.toSeconds());
    }

    private void touch(String key, String member) {
        long now = System.currentTimeMillis();
        redis.opsForZSet().add(key, member, now);
        redis.opsForZSet().removeRangeByScore(key, Double.NEGATIVE_INFINITY, now - ONLINE_WINDOW.toMillis());
        // Housekeeping only: if traffic stops entirely the set disappears instead of lingering.
        redis.expire(key, Duration.ofDays(1));
    }

    private long count(String key) {
        long since = System.currentTimeMillis() - ONLINE_WINDOW.toMillis();
        Long count = redis.opsForZSet().count(key, since, Double.POSITIVE_INFINITY);
        return count == null ? 0 : count;
    }
}
