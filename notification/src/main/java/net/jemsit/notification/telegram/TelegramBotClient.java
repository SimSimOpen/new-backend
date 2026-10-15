package net.jemsit.notification.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The handful of Telegram Bot API calls the OTP flow needs. */
@Component
@Slf4j
public class TelegramBotClient {

    private final String token;
    private final String username;
    private final RestClient http;

    public TelegramBotClient(@Value("${telegram.bot-token:}") String token,
                             @Value("${telegram.bot-username:}") String username) {
        this.token = token == null ? "" : token.trim();
        this.username = username == null ? "" : username.trim().replaceFirst("^@", "");
        this.http = RestClient.builder().baseUrl("https://api.telegram.org").build();
    }

    public boolean isConfigured() {
        return !token.isEmpty() && !username.isEmpty();
    }

    /** t.me deep link that opens the bot and sends it "/start {payload}". */
    public String startLink(String payload) {
        return "https://t.me/" + username + "?start=" + payload;
    }

    /** Long-polls for new updates; blocks for up to {@code timeoutSeconds}. */
    public List<Update> getUpdates(long offset, int timeoutSeconds) {
        ApiResponse<List<Update>> resp = http.get()
                .uri("/bot{token}/getUpdates?offset={offset}&timeout={timeout}&allowed_updates=[\"message\"]",
                        token, offset, timeoutSeconds)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return resp == null || resp.result() == null ? List.of() : resp.result();
    }

    /** getUpdates is refused while a webhook is set, so clear any left over from elsewhere. */
    public void deleteWebhook() {
        call("deleteWebhook", Map.of());
    }

    public void sendMessage(long chatId, String text) {
        sendMessage(chatId, text, Map.of("remove_keyboard", true));
    }

    /** Message with a one-tap "share my phone number" button. */
    public void askForContact(long chatId, String text, String buttonText) {
        Map<String, Object> button = Map.of("text", buttonText, "request_contact", true);
        sendMessage(chatId, text, Map.of(
                "keyboard", List.of(List.of(button)),
                "resize_keyboard", true,
                "one_time_keyboard", true));
    }

    private void sendMessage(long chatId, String text, Map<String, Object> replyMarkup) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("parse_mode", "HTML");
        body.put("reply_markup", replyMarkup);
        call("sendMessage", body);
    }

    private void call(String method, Map<String, Object> body) {
        http.post()
                .uri("/bot{token}/{method}", token, method)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiResponse<T>(boolean ok, T result) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Update(@JsonProperty("update_id") long updateId, Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(User from, Chat chat, String text, Contact contact) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(long id) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chat(long id, String type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(@JsonProperty("phone_number") String phoneNumber,
                          @JsonProperty("user_id") Long userId) {
    }
}
