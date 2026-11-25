package net.jemsit.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * usms.uz credentials and the approved OTP template. Only read when app.sms.provider=usms.
 *
 * @param baseUrl    API root, normally https://usms.uz/api/v1
 * @param login      integration login from the usms.uz cabinet
 * @param secret     integration secret from the usms.uz cabinet
 * @param from       approved sender name ("SMS nik"); empty sends without one
 * @param templateId id of the approved OTP template (kept as text so a bad value cannot stop startup)
 * @param codeParam  name of the template parameter that receives the code
 */
@ConfigurationProperties(prefix = "usms")
public record UsmsProperties(
        String baseUrl,
        String login,
        String secret,
        String from,
        String templateId,
        String codeParam
) {
}
