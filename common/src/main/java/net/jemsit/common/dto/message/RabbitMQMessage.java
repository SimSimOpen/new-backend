package net.jemsit.common.dto.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.jemsit.common.data.enums.RabbitMQMessages;
import lombok.Getter;

@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UserAvatarUpdated.class, name = "UserAvatarUpdated"),
        @JsonSubTypes.Type(value = MediaUploaded.class, name = "MediaUploaded"),
        @JsonSubTypes.Type(value = MediaFromMobileStarted.class, name = "MediaFromMobileStarted")
        // add all your subclasses here
})
public abstract class RabbitMQMessage {
    private String userId;
    private RabbitMQMessages message;
    private String userAvatarUrl;

    public RabbitMQMessage(String userId, RabbitMQMessages message) {
        this.userId = userId;
        this.message = message;
    }

    public RabbitMQMessage(String userId, String userAvatarUrl) {
        this.userId = userId;
        this.userAvatarUrl = userAvatarUrl;
    }

    protected RabbitMQMessage() {}

    public String getMessageString() {
        return message != null ? message.getMessage() : null; // ✅ null check
    }
}
