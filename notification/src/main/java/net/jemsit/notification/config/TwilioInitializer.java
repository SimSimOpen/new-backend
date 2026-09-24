package net.jemsit.notification.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "twilio")
public class TwilioInitializer {

    private final TwilioProperties twilioProperties;

    @PostConstruct
    public void init() {
        Twilio.init(twilioProperties.accountSid(), twilioProperties.authToken());
        log.info("Twilio initialized with account SID: {}", twilioProperties.accountSid());
    }
}
