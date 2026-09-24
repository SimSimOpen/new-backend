package net.jemsit.notification.service;

import net.jemsit.common.dto.response.auth.AuthenticationResponseDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationService {

    SseEmitter openStream(Long userId);

    void sendOtp(SmsRequestDTO request);

    AuthenticationResponseDTO verifyOtp(String phoneNumber, String otp);
}
