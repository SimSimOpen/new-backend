package net.jemsit.common.dto.message;

import lombok.Getter;

@Getter
public class UserAvatarUpdated {
    private final String userId;
    private final String userAvatarUrl;

    public UserAvatarUpdated(String userId, String userAvatarUrl) {
        this.userId = userId;
        this.userAvatarUrl = userAvatarUrl;
    }
}
