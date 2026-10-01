package net.jemsit.notify;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Holds the open sockets and keeps them alive.
 *
 * <p>Ported from the SimSim notification module's emitter registry, with one change that matters more
 * than any other line in this class. That registry logs {@code "No open SSE connection for user {},
 * dropping notification"} and returns when nobody is listening - which is correct for a browser tab,
 * where a closed tab means a user who left, and fatal for a phone, where a closed socket means a
 * device in Doze that will be back in four minutes and expects to have missed nothing.
 *
 * <p>Here, sending is an optimisation and the database is the delivery mechanism. A publish is
 * committed before anyone tries to send it, so "no socket open" costs nothing but latency: the next
 * reconnect replays it. That is why the no-listeners path below is a debug line and not a warning, and
 * why a failed {@code send()} completes the emitter instead of triggering any kind of retry - the
 * retry already happened, at the point the row was inserted.
 */
@Slf4j
public class NotifyStreamRegistry {

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    private final NotifyProperties properties;
    private final NotifyStore store;

    /**
     * Own scheduler rather than Spring's {@code @Scheduled}. Adding {@code @EnableScheduling} to an
     * auto-configuration would turn on scheduling for the entire host application and start picking up
     * any {@code @Scheduled} method that happens to be lying around in code that was never being run.
     * A module that changes its host's behaviour by existing is a module nobody wants to install.
     */
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "notify-sse");
                thread.setDaemon(true);
                return thread;
            });

    public NotifyStreamRegistry(NotifyProperties properties, NotifyStore store) {
        this.properties = properties;
        this.store = store;
    }

    @PostConstruct
    void start() {
        long heartbeat = properties.getHeartbeatMs();
        scheduler.scheduleAtFixedRate(this::heartbeats, heartbeat, heartbeat, TimeUnit.MILLISECONDS);
        long sweep = properties.getRetentionSweepMs();
        scheduler.scheduleAtFixedRate(this::sweep, sweep, sweep, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    void stop() {
        scheduler.shutdownNow();
    }

    public void register(String deviceId, SseEmitter emitter) {
        emitters.computeIfAbsent(deviceId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        log.debug("notify stream opened for device {}, {} open for that device",
                deviceId, emitters.get(deviceId).size());
    }

    public void unregister(String deviceId, SseEmitter emitter) {
        List<SseEmitter> open = emitters.get(deviceId);
        if (open == null) {
            return;
        }
        open.remove(emitter);
        if (open.isEmpty()) {
            emitters.remove(deviceId);
        }
        log.debug("notify stream closed for device {}, {} still open", deviceId, open.size());
    }

    /** Pushes one event to every open socket. */
    public int broadcast(SseEmitter.SseEventBuilder event) {
        return broadcast(event, null);
    }

    /**
     * Pushes one event to every open socket, or to {@code targets} when it is non-null and non-empty.
     *
     * <p>Returns the number of sockets the event was written to. Zero is a normal, expected outcome
     * and the caller must not treat it as a failure - see the class comment.
     */
    public int broadcast(SseEmitter.SseEventBuilder event, Set<String> targets) {
        if (emitters.isEmpty()) {
            return 0;
        }
        int sent = 0;
        for (Map.Entry<String, CopyOnWriteArrayList<SseEmitter>> entry : emitters.entrySet()) {
            if (targets != null && !targets.isEmpty() && !targets.contains(entry.getKey())) {
                continue;
            }
            for (SseEmitter emitter : entry.getValue()) {
                if (send(emitter, event)) {
                    sent++;
                }
            }
        }
        return sent;
    }

    public boolean isConnected(String deviceId) {
        List<SseEmitter> open = emitters.get(deviceId);
        return open != null && !open.isEmpty();
    }

    public int openDeviceCount() {
        return emitters.size();
    }

    public Set<String> connectedDeviceIds() {
        return Set.copyOf(emitters.keySet());
    }

    /**
     * Writes a heartbeat comment to every socket.
     *
     * <p>Two unrelated jobs, which is why the interval is short enough to look silly. It proves to the
     * client that a socket which has gone quiet is still live rather than half-open, and it keeps
     * traffic flowing so the NAT mapping the phone is reaching us through does not expire. Routers
     * commonly drop an idle UDP or TCP mapping after 30 seconds; {@code notify.heartbeat-ms} defaults
     * to 20 to sit inside that, and inside the 45 second tolerance the existing SimSim SSE client
     * already uses. Raising it past about 30 seconds without checking the router turns every idle
     * period into a silent disconnect.
     */
    private void heartbeats() {
        if (emitters.isEmpty()) {
            return;
        }
        broadcast(SseEmitter.event().comment("hb"), null);
    }

    /**
     * Deletes rows past the retention window.
     *
     * <p>Runs hourly by default against a 24 hour replay window and a 168 hour retention, so the table
     * holds roughly a week of history and the sweep removes rows no device was still allowed to see.
     * Retention has to exceed the replay window by a comfortable margin or the sweep deletes rows a
     * phone that was offline for two days would otherwise have been entitled to replay.
     */
    private void sweep() {
        try {
            Instant cutoff = Instant.now().minusMillis(properties.getRetentionHours() * 3_600_000L);
            int removed = store.deleteOlderThan(cutoff);
            if (removed > 0) {
                log.debug("notify retention sweep removed {} expired row(s)", removed);
            }
        } catch (RuntimeException e) {
            // Never let a sweep failure kill the scheduler thread. scheduleAtFixedRate cancels all
            // future runs if a task throws, which would silently stop heartbeats too - and a module
            // that stops heartbeating looks exactly like a module whose phones all disconnected.
            log.warn("notify retention sweep failed: {}", e.getMessage());
        }
    }

    /**
     * Writes to one socket, reporting whether it worked.
     *
     * <p>{@code IOException} means the peer is gone, so the emitter is completed - which fires its
     * onCompletion callback and unregisters it, keeping the map from filling with corpses.
     * {@code IllegalStateException} means it was already completed by another thread between our check
     * and our write; that is a race, not an error, and the right response is to do nothing.
     */
    private boolean send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
            return true;
        } catch (IOException e) {
            log.debug("notify send failed, completing emitter: {}", e.getMessage());
            emitter.complete();
            return false;
        } catch (IllegalStateException e) {
            log.debug("notify emitter already completed: {}", e.getMessage());
            return false;
        }
    }
}
