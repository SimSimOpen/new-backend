package net.jemsit.notification.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.common.exceptions.UserException;
import net.jemsit.notification.service.OtpStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * Delivers login codes through a Telegram bot.
 *
 * <p>A bot cannot message a phone number, only a chat that has talked to it. So the first time a number
 * is used, the site hands out a t.me link; the user opens the bot and shares their contact with the
 * one-tap button. Telegram only lets a user share their own verified number that way (we also check
 * {@code contact.user_id == from.id}), which links number -> chat and proves ownership. From then on,
 * codes for that number go straight to the chat.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramOtpService {

    private static final Duration LINK_TTL = Duration.ofMinutes(15);

    private final TelegramBotClient bot;
    private final OtpStore otpStore;
    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();

    public enum Status { SENT, LINK_REQUIRED }

    public record Result(Status status, String botUrl) {
    }

    /** Called by the website: send the code if the number is linked, otherwise return the bot link. */
    public Result requestCode(String rawPhone) {
        if (!bot.isConfigured()) {
            throw new UserException("Telegram login is not configured on the server");
        }
        String phone = OtpStore.normalizePhone(rawPhone);
        String chatId = redis.opsForValue().get(linkKey(phone));
        if (chatId != null) {
            sendCode(Long.parseLong(chatId), phone);
            return new Result(Status.SENT, null);
        }
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(pendingTokenKey(token), phone, LINK_TTL);
        return new Result(Status.LINK_REQUIRED, bot.startLink(token));
    }

    /** Called by the poller for every incoming message. */
    public void handle(TelegramBotClient.Message message) {
        if (message == null || message.chat() == null || !"private".equals(message.chat().type())) {
            return;
        }
        long chatId = message.chat().id();
        if (message.contact() != null) {
            onContact(chatId, message);
        } else if (message.text() != null && message.text().startsWith("/start")) {
            onStart(chatId, message.text().substring("/start".length()).trim());
        } else {
            bot.askForContact(chatId,
                    "To get SimSim login codes here, tap the button below to share your phone number.",
                    "📱 Share my phone number");
        }
    }

    private void onStart(long chatId, String token) {
        String phone = token.isEmpty() ? null : redis.opsForValue().getAndDelete(pendingTokenKey(token));
        if (phone != null) {
            // Remember which number this chat was asked about, to send the code once the contact arrives
            redis.opsForValue().set(pendingChatKey(chatId), phone, LINK_TTL);
        }
        bot.askForContact(chatId,
                "Welcome to <b>SimSim</b>!\n\nTap the button below to share your phone number. "
                        + "Your login code will arrive here.",
                "📱 Share my phone number");
    }

    private void onContact(long chatId, TelegramBotClient.Message message) {
        TelegramBotClient.Contact contact = message.contact();
        // Forwarded or typed-in contacts belong to someone else; only the button shares your own number
        if (message.from() == null || contact.userId() == null || contact.userId() != message.from().id()) {
            bot.askForContact(chatId, "Please share <b>your own</b> number using the button below.",
                    "📱 Share my phone number");
            return;
        }
        String phone = OtpStore.normalizePhone(contact.phoneNumber());
        redis.opsForValue().set(linkKey(phone), String.valueOf(chatId));
        log.info("Telegram chat linked to {}", phone);

        String requested = redis.opsForValue().getAndDelete(pendingChatKey(chatId));
        if (requested == null) {
            bot.sendMessage(chatId, "✅ Your number is linked. Choose <b>Telegram</b> on the SimSim login page "
                    + "and your codes will arrive here.");
        } else if (!requested.equals(phone)) {
            bot.sendMessage(chatId, "⚠️ This Telegram account's number is <b>" + phone + "</b>, but <b>" + requested
                    + "</b> was entered on the website. Go back and enter " + phone + ", or use SMS instead.");
        } else {
            try {
                sendCode(chatId, phone);
            } catch (UserException e) {
                bot.sendMessage(chatId, e.getMessage());
            }
        }
    }

    private void sendCode(long chatId, String phone) {
        String otp = otpStore.issue(phone);
        bot.sendMessage(chatId, "Your SimSim login code: <code>" + otp + "</code>\n\n"
                + "It is valid for 5 minutes. Do not share it with anyone.");
        log.info("OTP for {} sent via Telegram", phone);
    }

    private static String linkKey(String phone) {
        return "tg:link:" + phone;
    }

    private static String pendingTokenKey(String token) {
        return "tg:pending-token:" + token;
    }

    private static String pendingChatKey(long chatId) {
        return "tg:pending-chat:" + chatId;
    }
}
