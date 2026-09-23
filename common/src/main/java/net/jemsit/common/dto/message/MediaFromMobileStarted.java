package net.jemsit.common.dto.message;

import lombok.Getter;
import net.jemsit.common.data.enums.EventMessages;

@Getter
public class MediaFromMobileStarted {
    private final String userId;
    private final EventMessages message;

    public MediaFromMobileStarted(String userId, EventMessages message) {
        this.userId = userId;
        this.message = message;
    }
}
