package net.jemsit.notification.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.common.exceptions.UserException;
import net.jemsit.notification.config.UsmsProperties;
import net.jemsit.notification.service.SmsNotSentException;
import net.jemsit.notification.service.SmsRequestDTO;
import net.jemsit.notification.service.SmsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sends login codes through usms.uz (POST {baseUrl}/send). usms.uz only delivers approved templates,
 * so the code goes in as a template parameter rather than as free text.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "usms")
public class UsmsSmsSender implements SmsService {

    private final UsmsProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /** Null when the configuration is incomplete: SMS login is then reported as off. */
    private final Long templateId;

    public UsmsSmsSender(UsmsProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.templateId = parseTemplateId(props.templateId());
        // Never stop the whole app over SMS settings: log it and keep SMS login switched off
        if (isBlank(props.login()) || isBlank(props.secret()) || templateId == null) {
            log.error("SMS_PROVIDER=usms but USMS_LOGIN, USMS_SECRET or a numeric USMS_TEMPLATE_ID is missing"
                    + " (template id given: '{}'); SMS login stays off", props.templateId());
        }
    }

    @Override
    public boolean isEnabled() {
        return !isBlank(props.login()) && !isBlank(props.secret()) && templateId != null;
    }

    @Override
    public void sendSms(SmsRequestDTO request, String otp) {
        if (!isEnabled()) {
            throw new SmsNotSentException("usms.uz is not configured");
        }
        // usms.uz expects the number as digits only: 998901234567
        String phone = request.phoneNumber().replaceAll("\\D", "");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("phone", phone);
        if (!isBlank(props.from())) {
            body.put("from", props.from());
        }
        body.put("template_id", templateId);
        body.put("params", Map.of(codeParam(), otp));

        HttpResponse<String> response;
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(baseUrl() + "/send"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + props.login() + ":" + props.secret())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
            response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UserException("Could not send the SMS. Please try again in a minute");
        } catch (IOException e) {
            // The message may still have gone out: keep the resend cooldown instead of retrying
            log.error("usms.uz send to {} failed in transport: {}", phone, e.toString());
            throw new UserException("Could not send the SMS. Please try again in a minute");
        }

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            log.error("usms.uz refused SMS to {}: HTTP {} {}", phone, status, response.body());
            throw new SmsNotSentException("usms.uz answered HTTP " + status);
        }
        log.info("SMS sent to {} via usms.uz: {}", phone, summary(response.body()));
    }

    private String summary(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            return "id=" + node.path("id").asText("?") + " status=" + node.path("status").asText("?");
        } catch (IOException e) {
            return body;
        }
    }

    private String baseUrl() {
        String url = isBlank(props.baseUrl()) ? "https://usms.uz/api/v1" : props.baseUrl().trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String codeParam() {
        return isBlank(props.codeParam()) ? "code" : props.codeParam().trim();
    }

    private static Long parseTemplateId(String value) {
        try {
            return isBlank(value) ? null : Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
