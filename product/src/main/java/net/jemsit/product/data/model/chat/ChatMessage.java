package net.jemsit.product.data.model.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** One message in a listing chat. Rows are written with SQL in ChatService; this defines the table. */
@Entity
@Table(name = "chat_messages", indexes = @Index(columnList = "conversation_id, id"))
@Getter
@Setter
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;
    @Column(name = "sender_id", nullable = false)
    private Long senderId;
    @Column(nullable = false, length = ChatMessage.MAX_LENGTH)
    private String body;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static final int MAX_LENGTH = 2000;
}
