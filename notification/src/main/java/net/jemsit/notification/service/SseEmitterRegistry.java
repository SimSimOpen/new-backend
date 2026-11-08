package net.jemsit.notification.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseEmitterRegistry {
    void register(String userId, SseEmitter emitter);

    void unregister(String userId, SseEmitter emitter);

    void sendToUser(String userId, String message);

    /** Sends a named event whose data is serialized as JSON; dropped when the user has no open stream. */
    void sendEvent(String userId, String eventName, Object data);

    boolean isConnected(String userId);
}
