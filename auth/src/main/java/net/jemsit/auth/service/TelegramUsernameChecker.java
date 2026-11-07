package net.jemsit.auth.service;

import lombok.extern.slf4j.Slf4j;
import net.jemsit.auth.dto.TelegramAccountStatus;
import net.jemsit.auth.dto.TelegramCheckResponseDTO;
import net.jemsit.common.exceptions.UserException;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes Telegram usernames and works out what kind of account one points at, by reading its
 * public t.me page. The Bot API cannot answer this: getChat does not resolve personal usernames.
 *
 * <p>The page does not expose privacy settings, so a PERSONAL result does not guarantee that a
 * stranger's message will be delivered.
 */
@Service
@Slf4j
public class TelegramUsernameChecker {

    private static final Pattern LINK_PREFIX =
            Pattern.compile("^(https?://)?(www\\.)?(t\\.me|telegram\\.me)/", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID = Pattern.compile("^[A-Za-z](?!.*__)[A-Za-z0-9_]{2,30}[A-Za-z0-9]$");
    private static final Pattern ACTION = Pattern.compile("tgme_action_button_new[^>]*>([^<]+)<");
    private static final Pattern EXTRA = Pattern.compile("tgme_page_extra[^>]*>\\s*([^<]*)");

    private final RestClient http = RestClient.builder()
            .baseUrl("https://t.me")
            .requestFactory(requestFactory())
            .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; SimSim username check)")
            .build();

    private static JdkClientHttpRequestFactory requestFactory() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    /** Accepts "name", "@name" or a t.me link. Returns null for a blank value, which clears the field. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String value = LINK_PREFIX.matcher(raw.trim()).replaceFirst("");
        value = value.replaceFirst("^@", "").replaceFirst("/$", "");
        if (value.isEmpty()) {
            return null;
        }
        if (!VALID.matcher(value).matches()) {
            throw new UserException("Telegram username is not valid");
        }
        return value;
    }

    public TelegramCheckResponseDTO check(String raw) {
        String username = normalize(raw);
        if (username == null) {
            throw new UserException("Telegram username is not valid");
        }
        return new TelegramCheckResponseDTO(username, fetchStatus(username));
    }

    private TelegramAccountStatus fetchStatus(String username) {
        try {
            return classify(http.get().uri("/{username}", username).retrieve().body(String.class));
        } catch (RuntimeException e) {
            log.warn("Could not check Telegram username {}: {}", username, e.getMessage());
            return TelegramAccountStatus.UNKNOWN;
        }
    }

    static TelegramAccountStatus classify(String html) {
        if (html == null) {
            return TelegramAccountStatus.UNKNOWN;
        }
        if (html.contains("content=\"Telegram: Contact @")) {
            return TelegramAccountStatus.NOT_FOUND;
        }
        String action = firstGroup(ACTION, html);
        String extra = firstGroup(EXTRA, html);
        if (action.contains("Start Bot")) {
            return TelegramAccountStatus.BOT;
        }
        if (extra.contains("subscriber")) {
            return TelegramAccountStatus.CHANNEL;
        }
        if (extra.contains("member")) {
            return TelegramAccountStatus.GROUP;
        }
        return action.contains("Send Message") ? TelegramAccountStatus.PERSONAL : TelegramAccountStatus.UNKNOWN;
    }

    private static String firstGroup(Pattern pattern, String html) {
        Matcher matcher = pattern.matcher(html);
        return matcher.find() ? matcher.group(1) : "";
    }
}
