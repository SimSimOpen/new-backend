package net.jemsit.notify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;

/**
 * The one class a host project needs to know about. Inject it and call it:
 *
 * <pre>{@code
 * private final NotifyPublisher notify;
 *
 * notify.send("Build failed", "simsim-media on main");
 * notify.send("Media re-indexed", "412 files", "media-index", "low");
 * }</pre>
 *
 * <p>That is the entire integration surface. Everything else in this package is plumbing the host
 * never touches.
 *
 * <p><b>The ordering inside {@link #send(NotifyRequest)} is the design.</b> The row is committed first
 * and the socket write happens second, so a publish can never be lost by a send that fails. The
 * alternative order - write to whoever is connected, persist only if that fails - is what a
 * best-effort relay does, and it loses the notification whenever the phone is mid-handover between
 * cell towers, which on a mobile network is often. Committing first costs one insert per publish and
 * buys a relay that is correct regardless of who is listening.
 */
@Slf4j
public class NotifyPublisher {

    private final NotifyStore store;
    private final NotifyStreamRegistry streams;
    private final NotifyProperties properties;

    public NotifyPublisher(NotifyStore store,
                           NotifyStreamRegistry streams,
                           NotifyProperties properties) {
        this.store = store;
        this.streams = streams;
        this.properties = properties;
    }

    /** Publishes with no coalescing tag and default priority. */
    public long send(String title, String body) {
        return send(new NotifyRequest(title, body, null, null, null));
    }

    /** Publishes with a coalescing tag and a priority of {@code high} or {@code low}. */
    public long send(String title, String body, String tag, String priority) {
        return send(new NotifyRequest(title, body, tag, priority, null));
    }

    /**
     * Commits the notification and pushes it to any device that happens to be connected right now.
     *
     * @return the assigned id, which is also the cursor value a device will report once it has handled
     *         this notification. Returned so a caller can log it, not because anything needs to wait
     *         on it.
     */
    public long send(NotifyRequest request) {
        String title = trim(request.title(), properties.getTitleMaxLength());
        if (title.isEmpty()) {
            // Refusing rather than substituting a placeholder. A notification with no title renders as
            // an empty card the user cannot identify, and the caller has a bug worth finding.
            throw new IllegalArgumentException("notify title must not be blank");
        }

        String body = trim(request.body(), properties.getBodyMaxLength());
        String tag = trim(request.tag(), 64);
        String priority = normalisePriority(request.priority());
        String source = resolveSource(request.source());
        Instant now = Instant.now();
        // Fixed at publish time, not computed at replay time. Computing it lazily would mean a row's
        // visibility depended on when you asked, so two devices reconnecting a minute apart would
        // disagree about whether the same notification had expired.
        Instant expiresAt = now.plusMillis(properties.getMaxAgeMs());

        // The insert commits here, before anything is written to a socket. That ordering is the whole
        // design: a send that fails costs nothing because the row already exists and the next reconnect
        // replays it, whereas the reverse order loses the notification whenever the phone happens to be
        // mid-handover between towers - which on a mobile network is often.
        long id = store.insert(title, body, tag, priority, source, now, expiresAt);
        NotifyMessage message = new NotifyMessage(id, title, body, tag, priority, source, now);

        int delivered = streams.broadcast(event(message));
        if (delivered == 0) {
            // Not "dropped", which is the word the SimSim registry uses and the thing this module
            // exists to stop saying. The row is committed; whichever device this was for will collect
            // it on its next reconnect. Debug, because on a phone that is asleep most of the time this
            // is the common case and would otherwise be the noisiest line in the host's log.
            log.debug("notify {} queued, no device connected", id);
        } else {
            log.debug("notify {} pushed to {} socket(s)", id, delivered);
        }
        return id;
    }

    private SseEmitter.SseEventBuilder event(NotifyMessage message) {
        // id() is what makes this resumable: an SSE client that reconnects sends the last id it saw
        // back in a Last-Event-ID header, and the standard mechanism is the reason no custom protocol
        // is needed on the wire.
        return SseEmitter.event()
                .id(Long.toString(message.id()))
                .name("notification")
                .data(message, MediaType.APPLICATION_JSON);
    }

    private String resolveSource(String requested) {
        String value = trim(requested, 64);
        if (!value.isEmpty()) {
            return value;
        }
        String configured = properties.getSource();
        return configured == null || configured.isBlank() ? "notify" : configured.trim();
    }

    /** Anything unrecognised becomes {@code normal}. The phone branches on exactly two values and
     *  silently mis-routing a card to the heads-up channel because of a typo is worse than ignoring
     *  the typo. */
    private String normalisePriority(String requested) {
        String value = trim(requested, 16);
        if (value.isEmpty()) {
            return "normal";
        }
        String lower = value.toLowerCase();
        if (lower.equals("high") || lower.equals("max") || lower.equals("urgent")) {
            return "high";
        }
        if (lower.equals("low") || lower.equals("min") || lower.equals("quiet")) {
            return "low";
        }
        return "normal";
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        // An ellipsis rather than a hard cut, so a truncated body still reads as truncated instead of
        // ending mid-word in a way that looks like a rendering bug.
        return trimmed.substring(0, Math.max(0, maxLength - 1)) + "…";
    }
}
