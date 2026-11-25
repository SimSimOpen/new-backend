package net.jemsit.notification.service;

public interface SmsService {
    void sendSms(SmsRequestDTO request, String otp);

    /** Whether the sites should offer SMS login. False for the 'log' provider, which texts nobody. */
    default boolean isEnabled() {
        return true;
    }
}
