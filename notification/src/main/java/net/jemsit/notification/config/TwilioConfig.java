package net.jemsit.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({TwilioProperties.class, UsmsProperties.class})
public class TwilioConfig {
}
