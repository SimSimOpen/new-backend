package net.jemsit.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.common.api.AuthApi;
import net.jemsit.common.dto.message.MediaFromMobileStarted;
import net.jemsit.common.dto.message.MediaUploaded;
import net.jemsit.common.dto.response.auth.AuthenticationResponseDTO;
import net.jemsit.notification.service.NotificationService;
import net.jemsit.notification.service.OtpStore;
import net.jemsit.notification.service.SmsRequestDTO;
import net.jemsit.notification.service.SmsService;
import net.jemsit.notification.service.SseEmitterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SseEmitterRegistry sseEmitterRegistry;
    private final SmsService smsService;
    private final OtpStore otpStore;
    private final AuthApi authApi;

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
        String phoneNumber = OtpStore.normalizePhone(request.phoneNumber());
        String otp = otpStore.issue(phoneNumber);
        smsService.sendSms(new SmsRequestDTO(phoneNumber), otp);
        log.info("OTP issued for phone number {}", phoneNumber);
    }

    @Override
    public AuthenticationResponseDTO verifyOtp(String phoneNumber, String otp) {
        String key = OtpStore.normalizePhone(phoneNumber);
        otpStore.verify(key, otp);
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
