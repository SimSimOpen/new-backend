package net.jemsit.notification.service;

/**
 * The SMS provider answered and refused the message, so it was definitely not sent. A transport
 * failure (timeout, dropped connection) is reported differently: the message may have gone out, so
 * the caller must not resend it straight away.
 */
public class SmsNotSentException extends RuntimeException {
    public SmsNotSentException(String message) {
        super(message);
    }
}
