package net.jemsit.notification.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.jemsit.notification.service.SmsRequestDTO;
import net.jemsit.notification.service.SmsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "log", matchIfMissing = true)
public class LoggingSmsSender implements SmsService {

    /** Set app.sms.log-in-ui=true locally to try the SMS login screens with codes read from the log. */
    @Value("${app.sms.log-in-ui:false}")
    private boolean showInUi;

    @Override
    public boolean isEnabled() {
        return showInUi;
    }

    @Override
    public void sendSms(SmsRequestDTO request, String otp) {
        log.warn("SMS provider 'log' is active: OTP for {} is {} - no message was sent",
                request.phoneNumber(), otp);
    }
}
