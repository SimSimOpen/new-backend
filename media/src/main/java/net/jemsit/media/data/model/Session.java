package net.jemsit.media.data.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "sessions")
@Getter
@Setter
public class Session extends BaseEntity {
    private String sessionId;
    private Long userId;
    private Instant expiresAt;
}
