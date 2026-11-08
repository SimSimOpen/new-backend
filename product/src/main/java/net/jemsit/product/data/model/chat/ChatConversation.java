package net.jemsit.product.data.model.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * One signed-in user's chat with the agent of one listing. Rows are written with SQL in ChatService;
 * this entity only defines the table.
 */
@Entity
@Table(name = "chat_conversations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"kind", "listing_id", "client_id"}),
        indexes = {@Index(columnList = "client_id"), @Index(columnList = "agent_id")})
@Getter
@Setter
public class ChatConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String kind;
    @Column(name = "listing_id", nullable = false)
    private Long listingId;
    /** The user who started the chat from the public site. */
    @Column(name = "client_id", nullable = false)
    private Long clientId;
    /** The listing's owner at the time the chat started. */
    @Column(name = "agent_id", nullable = false)
    private Long agentId;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;
    /** Id of the last message each side has seen; anything newer from the other side is unread. */
    @Column(name = "client_read_id", nullable = false)
    private Long clientReadId;
    @Column(name = "agent_read_id", nullable = false)
    private Long agentReadId;
}
