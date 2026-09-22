package net.jemsit.common.dto.message;

import com.fasterxml.jackson.annotation.JsonTypeName;
import net.jemsit.common.data.enums.RabbitMQMessages;

@JsonTypeName("MediaFromMobileStarted")
public class MediaFromMobileStarted extends RabbitMQMessage {

    public MediaFromMobileStarted(String userId, RabbitMQMessages message) {
        super(userId, message);
    }

    protected MediaFromMobileStarted() {}
}
