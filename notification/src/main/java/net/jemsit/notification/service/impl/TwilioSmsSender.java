package net.jemsit.notification.service.impl;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.notification.config.TwilioProperties;
import net.jemsit.notification.service.SmsRequestDTO;
import net.jemsit.notification.service.SmsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service("twilio")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "twilio")
public class TwilioSmsSender implements SmsService {

    private final TwilioProperties twilioProperties;

    @Override
    public void sendSms(SmsRequestDTO request, String otp) {
        try {
            Message.creator(
                            new PhoneNumber(request.phoneNumber()),
                            new PhoneNumber(twilioProperties.trialNumber()),
                            " Your OTP is: " + otp)
                    .create();
            log.info("SMS sent to {}", request.phoneNumber());
        } catch (Exception e) {
            log.error("Failed to send SMS to {}. Error: {}", request.phoneNumber(), e.getMessage());
            throw new IllegalStateException("Failed to send SMS: " + e.getMessage(), e);
        }
    }
}
