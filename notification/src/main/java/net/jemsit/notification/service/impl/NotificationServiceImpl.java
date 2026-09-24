package net.jemsit.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.common.api.AuthApi;
import net.jemsit.common.dto.message.MediaFromMobileStarted;
import net.jemsit.common.dto.message.MediaUploaded;
import net.jemsit.common.dto.response.auth.AuthenticationResponseDTO;
import net.jemsit.common.exceptions.UserException;
import net.jemsit.notification.service.NotificationService;
import net.jemsit.notification.service.SmsRequestDTO;
import net.jemsit.notification.service.SmsService;
import net.jemsit.notification.service.SseEmitterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Duration OTP_TTL = Duration.ofMinutes(2);

    private final SseEmitterRegistry sseEmitterRegistry;
    private final SmsService smsService;
    private final StringRedisTemplate redisTemplate;
    private final AuthApi authApi;
    private final SecureRandom random = new SecureRandom();

    @Override
    public SseEmitter openStream(Long userId) {
        String key = String.valueOf(userId);
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> sseEmitterRegistry.unregister(key, emitter));
        emitter.onTimeout(() -> {
            sseEmitterRegistry.unregister(key, emitter);
            emitter.complete();
        });
        emitter.onError(error -> sseEmitterRegistry.unregister(key, emitter));
        sseEmitterRegistry.register(key, emitter);
        try {
            emitter.send(SseEmitter.event().name("connected").data("Connection established"));
        } catch (IOException e) {
            sseEmitterRegistry.unregister(key, emitter);
            emitter.completeWithError(e);
        }
        log.info("SSE stream opened for user {}", userId);
        return emitter;
    }

    @Override
    public void sendOtp(SmsRequestDTO request) {
        String phoneNumber = request.phoneNumber().trim();
        String otp = String.format("%06d", random.nextInt(1_000_000));
        redisTemplate.delete(phoneNumber);
        redisTemplate.opsForList().rightPush(phoneNumber, otp);
        redisTemplate.expire(phoneNumber, OTP_TTL);
        smsService.sendSms(new SmsRequestDTO(phoneNumber), otp);
        log.info("OTP issued for phone number {}", phoneNumber);
    }

    @Override
    public AuthenticationResponseDTO verifyOtp(String phoneNumber, String otp) {
        String key = phoneNumber.trim();
        String storedOtp = redisTemplate.opsForList().leftPop(key);
        if (storedOtp == null) {
            log.warn("No OTP stored for phone number: {}", key);
            throw new UserException("No OTP found for the provided phone number");
        }
        if (!storedOtp.equals(otp)) {
            log.warn("Invalid OTP attempt for phone number: {}", key);
            throw new UserException("Invalid OTP provided");
        }
        log.info("OTP verified for phone number {}", key);
        return authApi.authenticateWithOtp(key);
    }

    @EventListener
    public void onMediaUploaded(MediaUploaded event) {
        notifyUser(event.getUserId(), event.getMessage().getMessage());
    }

    @EventListener
    public void onMediaFromMobileStarted(MediaFromMobileStarted event) {
        notifyUser(event.getUserId(), event.getMessage().getMessage());
    }

    private void notifyUser(String userId, String message) {
        sseEmitterRegistry.sendToUser(userId, message);
    }
}
