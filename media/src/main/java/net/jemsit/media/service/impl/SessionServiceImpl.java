package net.jemsit.media.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.common.UserContext;
import net.jemsit.common.data.enums.EventMessages;
import net.jemsit.common.dto.message.MediaFromMobileStarted;
import net.jemsit.common.dto.response.media.SessionResponseDTO;
import net.jemsit.common.exceptions.UserException;
import net.jemsit.media.data.model.Session;
import net.jemsit.media.data.repository.SessionRepository;
import net.jemsit.media.mapper.SessionMapper;
import net.jemsit.media.service.SessionService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final SessionMapper sessionMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public SessionResponseDTO createUploadSession() {
        Session session = new Session();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(UserContext.getUserId());
        session.setExpiresAt(Instant.now().plus(Duration.ofMinutes(15)));
        return sessionMapper.toDto(sessionRepository.save(session));
    }

    @Override
    public void getSession(String sessionId) {
        Session session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new UserException("Session not found"));

        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new UserException("Session expired");
        }
        eventPublisher.publishEvent(
                new MediaFromMobileStarted(String.valueOf(session.getUserId()), EventMessages.MOBILE_SESSION_STARTED));
    }
}
