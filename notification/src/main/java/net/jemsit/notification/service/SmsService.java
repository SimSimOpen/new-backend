package net.jemsit.notification.service;

public interface SmsService {
    void sendSms(SmsRequestDTO request, String otp);
}
