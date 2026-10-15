package net.jemsit.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.common.exceptions.UserException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * One-time codes, shared by every delivery channel (SMS provider, Telegram bot). A code is issued for
 * a phone number and can be checked a few times before it is thrown away, so a typo does not force a
 * resend but guessing all million codes is still impossible.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OtpStore {

    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();

    /** "+998 90 123-45-67" / "998901234567" -> "+998901234567", the form accounts are keyed by. */
    public static String normalizePhone(String phoneNumber) {
        String digits = phoneNumber == null ? "" : phoneNumber.replaceAll("\\D", "");
        if (digits.length() < 7) {
            throw new UserException("Enter a valid phone number");
        }
        return "+" + digits;
    }

    /** Creates a new code for the number, replacing any earlier one. Throws while the cooldown runs. */
    public String issue(String phone) {
        Boolean first = redis.opsForValue().setIfAbsent(cooldownKey(phone), "1", RESEND_COOLDOWN);
        if (!Boolean.TRUE.equals(first)) {
            throw new UserException("Please wait a minute before requesting another code");
        }
        String otp = String.format("%06d", random.nextInt(1_000_000));
        redis.opsForValue().set(codeKey(phone), otp, OTP_TTL);
        redis.delete(attemptsKey(phone));
        return otp;
    }

    /** Consumes the code on success; throws with a user-facing message otherwise. */
    public void verify(String phone, String otp) {
        String stored = redis.opsForValue().get(codeKey(phone));
        if (stored == null) {
            throw new UserException("The code has expired. Please request a new one");
        }
        if (!stored.equals(otp == null ? "" : otp.trim())) {
            Long attempts = redis.opsForValue().increment(attemptsKey(phone));
            redis.expire(attemptsKey(phone), OTP_TTL);
            if (attempts != null && attempts >= MAX_ATTEMPTS) {
                redis.delete(codeKey(phone));
                log.warn("OTP for {} discarded after {} wrong attempts", phone, attempts);
                throw new UserException("Too many wrong codes. Please request a new one");
            }
            throw new UserException("That code is incorrect");
        }
        redis.delete(codeKey(phone));
        redis.delete(attemptsKey(phone));
    }

    private static String codeKey(String phone) {
        return "otp:code:" + phone;
    }

    private static String attemptsKey(String phone) {
        return "otp:attempts:" + phone;
    }

    private static String cooldownKey(String phone) {
        return "otp:cooldown:" + phone;
    }
}
