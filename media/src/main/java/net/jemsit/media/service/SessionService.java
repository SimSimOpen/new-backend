package net.jemsit.media.service;

import net.jemsit.common.dto.response.media.SessionResponseDTO;

public interface SessionService {
    SessionResponseDTO createUploadSession();

    void getSession(String sessionId);
}
