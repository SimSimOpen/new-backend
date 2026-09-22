package net.jemsit.common.data.enums;

import lombok.Getter;

@Getter
public enum RabbitMQMessages {
    MOBILE_SESSION_STARTED("Mobile session started"),
    MEDIA_UPDATE("Media updated"),
    USER_AVATAR_UPDATED("User avatar updated");

    private final String message;

    RabbitMQMessages(String message) {
        this.message = message;
    }
}
