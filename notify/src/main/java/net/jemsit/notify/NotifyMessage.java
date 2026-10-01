package net.jemsit.notify;

import java.time.Instant;

/**
 * The payload that actually goes down the wire, and the shape of the {@code notify_outbox} row it was
 * built from.
 *
 * <p>Sending the whole object rather than a bare string is the point. The existing SimSim emitter
 * sends {@code event().data("Media updated")} - a sentence with no identity - so the phone cannot tell
 * two notifications apart, cannot replace one card with a newer version of itself, and cannot know
 * which backend it came from when several are configured. Every field here exists to answer one of
 * those:
 *
 * <ul>
 *   <li>{@code id} - the cursor. The phone reports the highest one it handled, so this is what makes
 *       the stream resumable rather than best-effort.</li>
 *   <li>{@code tag} - coalescing. The phone derives its Android notification id from this, so two
 *       publishes with the same tag update one card in place instead of stacking.</li>
 *   <li>{@code priority} - {@code high} routes to the heads-up channel, anything else to the silent
 *       one. Letting the backend choose means a CI failure can buzz and a media re-index cannot.</li>
 *   <li>{@code source} - which backend, for grouping in the inbox and for the per-source channel.</li>
 *   <li>{@code at} - when it was published, not when it arrived. After a long offline gap the two
 *       differ by hours and "2 minutes ago" on a stale card is simply wrong.</li>
 * </ul>
 */
public record NotifyMessage(long id,
                            String title,
                            String body,
                            String tag,
                            String priority,
                            String source,
                            Instant at) {
}
