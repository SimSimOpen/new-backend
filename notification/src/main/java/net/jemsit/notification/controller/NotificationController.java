package net.jemsit.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.common.UserContext;
import net.jemsit.common.dto.response.auth.AuthenticationResponseDTO;
import net.jemsit.notification.service.NotificationService;
import net.jemsit.notification.service.SmsRequestDTO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/event")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        // UserContext is a ThreadLocal cleared once the request thread returns, so read it here
        return notificationService.openStream(UserContext.getUserId());
    }

    @PostMapping("/send-otp")
    public Map<String, Object> sendOtp(@RequestBody SmsRequestDTO request) {
        notificationService.sendOtp(request);
        return Map.of("success", true, "message", "OTP sent successfully");
    }

    @GetMapping("/verify-otp")
    public AuthenticationResponseDTO verifyOtp(@RequestParam String phoneNumber, @RequestParam String otp) {
        return notificationService.verifyOtp(phoneNumber, otp);
    }
}
