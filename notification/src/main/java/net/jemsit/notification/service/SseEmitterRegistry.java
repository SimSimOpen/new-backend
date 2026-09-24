package net.jemsit.notification.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseEmitterRegistry {
    void register(String userId, SseEmitter emitter);

    void unregister(String userId, SseEmitter emitter);

    void sendToUser(String userId, String message);

    boolean isConnected(String userId);
}
