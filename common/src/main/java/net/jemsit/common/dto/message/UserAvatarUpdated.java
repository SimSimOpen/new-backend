package net.jemsit.common.dto.message;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("UserAvatarUpdated")
public class UserAvatarUpdated extends RabbitMQMessage {

    public UserAvatarUpdated(String userId,  String avatarUrl) {
        super(userId, avatarUrl);
    }

    protected UserAvatarUpdated() {}


}
