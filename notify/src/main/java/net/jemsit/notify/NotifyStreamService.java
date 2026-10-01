package net.jemsit.notify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Opens streams and replays what a device missed while it was away.
 *
 * <p>This class is the reason SSE is a safe transport for a phone rather than merely a convenient one.
 * Plain SSE is best-effort: if the socket is down at the moment an event is written, the event is gone
 * and nothing knows it. Every guarantee here comes from combining the socket with the outbox table and
 * a per-device cursor, so the socket only ever has to be <em>fast</em> and never has to be
 * <em>reliable</em>.
 *
 * <p>It also removes a failure mode the existing SimSim registry has. That one writes to whichever
 * sockets happen to be open and, finding none, logs {@code "dropping notification"}. On a phone there
 * is no socket open most of the time - Doze, radio handover, a cached process - so "nobody listening"
 * is the normal case rather than the exceptional one, and treating it as a terminal outcome loses
 * nearly everything.
 */
@Slf4j
public class NotifyStreamService {

    private final NotifyStore store;
    private final NotifyStreamRegistry streams;
    private final NotifyProperties properties;

    public NotifyStreamService(NotifyStore store,
                               NotifyStreamRegistry streams,
                               NotifyProperties properties) {
        this.store = store;
        this.streams = streams;
        this.properties = properties;
    }

    /**
     * Opens a stream for one device, replays everything it has not seen, then leaves the socket open
     * for live events.
     *
     * @param since       cursor the client states explicitly, or null. Wins over the header, because a
     *                    hand-rolled client can be precise about it whereas a browser's
     *                    {@code Last-Event-ID} is whatever the browser happened to remember.
     * @param lastEventId value of the standard SSE resume header, or null.
     */
    public SseEmitter openStream(String deviceId, String label, Long since, String lastEventId) {
        long stored = storedCursor(deviceId, label);
        long cursor = resolveCursor(since, lastEventId, stored);

        // Infinite timeout, matching the existing SimSim emitter. A finite one fires onTimeout during a
        // quiet hour and drops a perfectly healthy connection; proving liveness is the heartbeat's job.
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> streams.unregister(deviceId, emitter));
        emitter.onTimeout(() -> {
            streams.unregister(deviceId, emitter);
            emitter.complete();
        });
        emitter.onError(error -> streams.unregister(deviceId, emitter));

        // Registered before the replay, not after. A publish landing between the query and the
        // registration would be missed entirely, whereas one landing between the registration and the
        // query is delivered twice - and a duplicate is harmless, because the phone derives its
        // notification id from the tag so the second copy replaces the first in place. This ordering is
        // the difference between at-least-once and at-most-once, and only one of those is acceptable
        // for something whose entire purpose is not losing messages.
        streams.register(deviceId, emitter);

        Replay replay;
        try {
            send(emitter, SseEmitter.event().name("connected").data(payload("connected", deviceId, cursor)));
            replay = replay(emitter, cursor);
            send(emitter, SseEmitter.event().name("sync").data(syncPayload(deviceId, replay)));
        } catch (RuntimeException e) {
            // The client vanished mid-handshake. Unregister and complete, so the map does not keep an
            // emitter that can never be written to again.
            log.debug("notify stream setup failed for device {}: {}", deviceId, e.getMessage());
            streams.unregister(deviceId, emitter);
            emitter.completeWithError(e);
            return emitter;
        }

        log.info("notify stream open for device {}: resumed at {}, replayed {}, cursor now {}",
                deviceId, cursor, replay.count(), replay.cursor());
        return emitter;
    }

    /**
     * Records how far a device has got.
     *
     * <p>Monotonic, because acknowledgements arrive over a network that reorders and duplicates them.
     *
     * @return the cursor now stored, which is {@code max(existing, incoming)} and may therefore differ
     *         from what the caller sent. Returned rather than a bare ok so a client can notice it is
     *         behind its own view - which happens when two processes share a device id, or when a stale
     *         ack arrived out of order.
     */
    public long recordCursor(String deviceId, long cursor, String label) {
        try {
            return store.writeCursor(deviceId, label, cursor);
        } catch (DataAccessException e) {
            // Cursor persistence is a convenience for a client that has lost its own state. Failing the
            // request would tell the phone its notifications were not delivered when they were, which is
            // the one thing this module must never say.
            log.warn("could not persist cursor {} for device {}: {}", cursor, deviceId, e.getMessage());
            return Math.max(0L, cursor);
        }
    }

    public Map<String, Object> health() {
        Instant now = Instant.now();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "up");
        payload.put("source", properties.getSource());
        payload.put("devices", store.deviceCount());
        payload.put("connected", streams.openDeviceCount());
        payload.put("replayable", store.countReplayable(now));
        payload.put("maxId", store.findMaxId());
        payload.put("maxAgeHours", properties.getMaxAgeMs() / 3_600_000L);
        payload.put("heartbeatSeconds", properties.getHeartbeatMs() / 1000L);
        payload.put("now", now.toString());
        return payload;
    }

    /**
     * Every device this backend has ever heard from, with how far behind each one is.
     *
     * <p>{@code behind} answers the only question anyone actually asks a notification relay - "is my
     * phone current?" - and it is answerable at all precisely because progress is one integer per
     * device rather than a row per device per notification.
     */
    public List<Map<String, Object>> deviceList() {
        long head = store.findMaxId();
        List<Map<String, Object>> result = new ArrayList<>();
        for (NotifyStore.DeviceRow device : store.devices()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("deviceId", device.deviceId());
            entry.put("label", device.label());
            entry.put("cursor", device.cursor());
            entry.put("behind", Math.max(0L, head - device.cursor()));
            entry.put("connected", streams.isConnected(device.deviceId()));
            entry.put("registeredAt", device.registeredAt());
            entry.put("lastSeenAt", device.lastSeenAt());
            result.add(entry);
        }
        return result;
    }

    /**
     * Writes every unexpired row above the cursor, in id order, and reports the cursor the device
     * should hold afterwards.
     *
     * <p>Capped by {@code notify.max-replay}. A device away long enough to exceed the cap does not lose
     * the remainder: the returned cursor stops at the last row actually sent and {@code truncated} is
     * set, so the phone reconnects and continues from there. Truncating without advancing the cursor
     * across the gap is what keeps a cap from becoming a silent drop.
     */
    private Replay replay(SseEmitter emitter, long cursor) {
        int cap = properties.getMaxReplay();
        List<NotifyMessage> rows = store.findReplayable(cursor, Instant.now(), cap);
        long latest = cursor;
        for (NotifyMessage row : rows) {
            send(emitter, SseEmitter.event()
                    .id(Long.toString(row.id()))
                    .name("notification")
                    .data(row, MediaType.APPLICATION_JSON));
            latest = Math.max(latest, row.id());
        }

        boolean truncated = rows.size() >= cap;
        if (!truncated) {
            // Caught up, so jump to the head of the table rather than to the last row sent. Otherwise a
            // device whose gap is entirely expired rows would rediscover the same empty gap on every
            // reconnect for the rest of the retention window.
            latest = Math.max(latest, store.findMaxId());
        }
        if (!rows.isEmpty()) {
            log.info("replayed {} notification(s), cursor {} -> {}", rows.size(), cursor, latest);
        }
        return new Replay(rows.size(), latest, truncated);
    }

    private Map<String, Object> syncPayload(String deviceId, Replay replay) {
        Map<String, Object> payload = payload("sync", deviceId, replay.cursor());
        payload.put("replayed", replay.count());
        payload.put("truncated", replay.truncated());
        return payload;
    }

    private Map<String, Object> payload(String event, String deviceId, long cursor) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("event", event);
        map.put("deviceId", deviceId);
        map.put("cursor", cursor);
        map.put("source", properties.getSource());
        return map;
    }

    /**
     * Resolves where to start replaying: explicit parameter, then the standard header, then whatever
     * the server remembers, then zero.
     *
     * <p>The stored cursor is what makes an APK reinstall survivable. A client that keeps its device id
     * picks up where it left off instead of replaying a full retention window of stale cards.
     */
    private long resolveCursor(Long since, String lastEventId, long stored) {
        if (since != null && since >= 0L) {
            return since;
        }
        if (lastEventId != null && !lastEventId.isBlank()) {
            try {
                return Long.parseLong(lastEventId.trim());
            } catch (NumberFormatException e) {
                log.debug("ignoring unparseable Last-Event-ID header: {}", lastEventId);
            }
        }
        return stored;
    }

    /**
     * Loads or creates the device row, stamps {@code lastSeenAt}, and reports the cursor the server
     * remembers.
     *
     * <p>Every database failure here is swallowed and reported as zero. Registration is bookkeeping,
     * and refusing to open a stream because a bookkeeping write failed would be exactly the kind of
     * self-inflicted loss this module exists to prevent. Zero is also the safe direction to fail in: it
     * replays too much rather than too little, and a duplicate replaces its own card.
     */
    private long storedCursor(String deviceId, String label) {
        try {
            return store.readCursor(deviceId, label);
        } catch (DataAccessException e) {
            log.warn("could not register device {}: {}", deviceId, e.getMessage());
            return 0L;
        }
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        // Sends made before the handler returns are buffered by ResponseBodyEmitter and flushed in
        // order once the async response is initialised, so writing the replay here cannot reorder it
        // against anything the live path pushes afterwards.
        try {
            emitter.send(event);
        } catch (IOException e) {
            throw new IllegalStateException("SSE client disconnected during setup", e);
        }
    }

    /** Outcome of one replay pass: how many rows went out, the cursor they imply, and whether the cap
     *  was hit. */
    private record Replay(int count, long cursor, boolean truncated) {
    }
}
