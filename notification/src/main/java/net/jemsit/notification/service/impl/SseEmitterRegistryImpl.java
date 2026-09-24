package net.jemsit.notification.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.notification.service.SseEmitterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SseEmitterRegistryImpl implements SseEmitterRegistry {

    // the realtor client closes the stream after 45s without traffic (heartbeatTimeout)
    private static final long HEARTBEAT_SECONDS = 20;

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatScheduler =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "sse-heartbeat");
                thread.setDaemon(true);
                return thread;
            });

    @PostConstruct
    void startHeartbeat() {
        heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeats,
                HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stopHeartbeat() {
        heartbeatScheduler.shutdownNow();
    }

    @Override
    public void register(String userId, SseEmitter emitter) {
        emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("Registered SSE emitter for user {}. Open emitters: {}", userId, emitters.get(userId).size());
    }

    @Override
    public void unregister(String userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emitters.remove(userId);
        }
        log.info("Unregistered SSE emitter for user {}. Open emitters: {}", userId, userEmitters.size());
    }

    @Override
    public void sendToUser(String userId, String message) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            log.info("No open SSE connection for user {}, dropping notification: {}", userId, message);
            return;
        }
        log.info("Sending notification to user {}: {}", userId, message);
        userEmitters.forEach(emitter ->
                send(emitter, SseEmitter.event().name("media-notification").data(message)));
    }

    @Override
    public boolean isConnected(String userId) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        return userEmitters != null && !userEmitters.isEmpty();
    }

    private void sendHeartbeats() {
        emitters.forEach((userId, userEmitters) -> userEmitters.forEach(emitter ->
                send(emitter, SseEmitter.event().name("heartbeat").data("ping"))));
    }

    private void send(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException e) {
            log.debug("SSE send failed, completing emitter: {}", e.getMessage());
            emitter.complete();
        } catch (IllegalStateException e) {
            log.debug("SSE emitter already completed: {}", e.getMessage());
        }
    }
}
