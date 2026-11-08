package net.jemsit.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.common.UserContext;
import net.jemsit.common.api.AuthApi;
import net.jemsit.common.dto.message.ChatEvent;
import net.jemsit.common.dto.response.auth.ProfileResponseDTO;
import net.jemsit.common.dto.response.auth.UserDetailsResponseDTO;
import net.jemsit.common.exceptions.UserException;
import net.jemsit.product.data.model.chat.ChatMessage;
import net.jemsit.product.data.model.engagement.ListingKind;
import net.jemsit.product.dto.chat.ChatEventPayload;
import net.jemsit.product.dto.chat.ChatMessageDTO;
import net.jemsit.product.dto.chat.ChatRole;
import net.jemsit.product.dto.chat.ConversationDTO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Chats between a signed-in user and the agent of a listing, one conversation per user and listing.
 * Every endpoint acts as the caller in {@link UserContext}; a conversation is visible only to its two
 * participants. New messages are pushed to both of them through the notification module's SSE stream.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private static final int PAGE_SIZE = 50;
    private static final int MAX_CONVERSATIONS = 100;

    /** Titles stay readable after a listing is deleted, so deleted rows are included here. */
    private static final String LISTINGS = Arrays.stream(ListingKind.values())
            .map(kind -> "SELECT '" + kind.name() + "' AS kind, id, title, agent FROM " + kind.table())
            .collect(Collectors.joining(" UNION ALL "));

    private static final String CONVERSATIONS = """
            SELECT c.id, c.kind, c.listing_id, c.client_id, c.agent_id, c.created_at,
                   l.title AS listing_title, l.agent AS agent_name,
                   m.body AS last_body, m.sender_id AS last_sender, m.created_at AS last_at,
                   (SELECT COUNT(*) FROM chat_messages x
                     WHERE x.conversation_id = c.id AND x.sender_id <> :me
                       AND x.id > CASE WHEN c.client_id = :me THEN c.client_read_id ELSE c.agent_read_id END) AS unread
            FROM chat_conversations c
            LEFT JOIN (""" + LISTINGS + """
            ) l ON l.kind = c.kind AND l.id = c.listing_id
            LEFT JOIN LATERAL (SELECT body, sender_id, created_at FROM chat_messages
                                WHERE conversation_id = c.id ORDER BY id DESC LIMIT 1) m ON true
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final AuthApi authApi;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Opens the caller's conversation about a live listing, creating it on first use. Agents see a
     * conversation only once it has a message, so opening the chat window alone does not disturb them.
     */
    @Transactional
    public ConversationDTO start(ListingKind kind, Long listingId) {
        Long me = requireUser();
        List<Long> owners = jdbc.queryForList("SELECT agentid FROM " + kind.table()
                        + " WHERE id = :listingId AND listing_status = 'ACTIVE' AND is_deleted = false",
                new MapSqlParameterSource("listingId", listingId), Long.class);
        if (owners.isEmpty() || owners.getFirst() == null) {
            throw new UserException("This listing is not available");
        }
        Long agentId = owners.getFirst();
        if (agentId.equals(me)) {
            throw new UserException("You cannot start a chat about your own listing");
        }
        jdbc.update("""
                INSERT INTO chat_conversations (kind, listing_id, client_id, agent_id, created_at, client_read_id, agent_read_id)
                VALUES (:kind, :listingId, :me, :agentId, now(), 0, 0)
                ON CONFLICT (kind, listing_id, client_id) DO NOTHING""",
                new MapSqlParameterSource()
                        .addValue("kind", kind.name())
                        .addValue("listingId", listingId)
                        .addValue("me", me)
                        .addValue("agentId", agentId));
        Long id = jdbc.queryForObject("""
                SELECT id FROM chat_conversations WHERE kind = :kind AND listing_id = :listingId AND client_id = :me""",
                new MapSqlParameterSource()
                        .addValue("kind", kind.name())
                        .addValue("listingId", listingId)
                        .addValue("me", me), Long.class);
        return get(id);
    }

    /** The caller's conversations, most recently active first. */
    @Transactional(readOnly = true)
    public List<ConversationDTO> list() {
        Long me = requireUser();
        return toDtos(jdbc.queryForList(CONVERSATIONS + """
                WHERE c.client_id = :me OR (c.agent_id = :me AND c.last_message_at IS NOT NULL)
                ORDER BY COALESCE(c.last_message_at, c.created_at) DESC, c.id DESC
                LIMIT""" + " " + MAX_CONVERSATIONS, new MapSqlParameterSource("me", me)), me);
    }

    @Transactional(readOnly = true)
    public ConversationDTO get(Long conversationId) {
        Long me = requireUser();
        List<ConversationDTO> found = toDtos(jdbc.queryForList(CONVERSATIONS
                        + " WHERE c.id = :id AND (c.client_id = :me OR c.agent_id = :me)",
                new MapSqlParameterSource().addValue("id", conversationId).addValue("me", me)), me);
        if (found.isEmpty()) {
            throw new UserException("Conversation not found");
        }
        return found.getFirst();
    }

    /** Up to one page of messages older than {@code before} (or the newest), oldest first. */
    @Transactional(readOnly = true)
    public List<ChatMessageDTO> messages(Long conversationId, Long before) {
        Participants p = requireParticipant(conversationId);
        List<ChatMessageDTO> page = new ArrayList<>(jdbc.query("""
                        SELECT id, conversation_id, sender_id, body, created_at FROM chat_messages
                        WHERE conversation_id = :id AND (CAST(:before AS bigint) IS NULL OR id < :before)
                        ORDER BY id DESC LIMIT""" + " " + PAGE_SIZE,
                new MapSqlParameterSource().addValue("id", conversationId).addValue("before", before),
                (rs, i) -> toMessage(rs, p)));
        Collections.reverse(page);
        return page;
    }

    @Transactional
    public ChatMessageDTO send(Long conversationId, String body) {
        Participants p = requireParticipant(conversationId);
        String text = body == null ? "" : body.strip();
        if (text.isEmpty() || text.length() > ChatMessage.MAX_LENGTH) {
            throw new UserException("A message must be 1 to " + ChatMessage.MAX_LENGTH + " characters long");
        }
        ChatMessageDTO message = jdbc.queryForObject("""
                        INSERT INTO chat_messages (conversation_id, sender_id, body, created_at)
                        VALUES (:id, :me, :body, now())
                        RETURNING id, conversation_id, sender_id, body, created_at""",
                new MapSqlParameterSource()
                        .addValue("id", conversationId)
                        .addValue("me", p.me())
                        .addValue("body", text),
                (rs, i) -> toMessage(rs, p));
        // The sender has obviously seen their own message.
        jdbc.update("UPDATE chat_conversations SET last_message_at = now(), "
                        + readColumn(p.myRole()) + " = :messageId WHERE id = :id",
                new MapSqlParameterSource().addValue("id", conversationId).addValue("messageId", message.id()));
        // Both sides get the event: the other participant, and the sender's other open tabs.
        publishAfterCommit(new ChatEvent(List.of(p.clientId(), p.agentId()), ChatEventPayload.message(message)));
        return message;
    }

    /** Marks everything in the conversation as seen by the caller. */
    @Transactional
    public void markRead(Long conversationId) {
        Participants p = requireParticipant(conversationId);
        jdbc.update("UPDATE chat_conversations SET " + readColumn(p.myRole())
                        + " = COALESCE((SELECT MAX(id) FROM chat_messages WHERE conversation_id = :id), 0) WHERE id = :id",
                new MapSqlParameterSource("id", conversationId));
    }

    /** Total unread messages across the caller's conversations, for a badge. */
    @Transactional(readOnly = true)
    public long unreadCount() {
        Long me = requireUser();
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM chat_messages x JOIN chat_conversations c ON c.id = x.conversation_id
                WHERE (c.client_id = :me OR c.agent_id = :me) AND x.sender_id <> :me
                  AND x.id > CASE WHEN c.client_id = :me THEN c.client_read_id ELSE c.agent_read_id END""",
                new MapSqlParameterSource("me", me), Long.class);
        return count == null ? 0 : count;
    }

    private List<ConversationDTO> toDtos(List<Map<String, Object>> rows, Long me) {
        Map<Long, String> clientNames = new HashMap<>();
        List<ConversationDTO> result = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Long clientId = ((Number) row.get("client_id")).longValue();
            ChatRole role = clientId.equals(me) ? ChatRole.CLIENT : ChatRole.AGENT;
            String counterpart = role == ChatRole.CLIENT
                    ? (String) row.get("agent_name")
                    : clientNames.computeIfAbsent(clientId, this::displayName);
            Number lastSender = (Number) row.get("last_sender");
            result.add(new ConversationDTO(
                    ((Number) row.get("id")).longValue(),
                    ListingKind.valueOf((String) row.get("kind")),
                    ((Number) row.get("listing_id")).longValue(),
                    (String) row.get("listing_title"),
                    role,
                    counterpart,
                    (String) row.get("last_body"),
                    lastSender == null ? null
                            : lastSender.longValue() == clientId ? ChatRole.CLIENT : ChatRole.AGENT,
                    toInstant(row.get("last_at")),
                    ((Number) row.get("unread")).longValue()));
        }
        return result;
    }

    /** Full name from the profile when there is one, otherwise the username (a phone number for clients). */
    private String displayName(Long userId) {
        try {
            UserDetailsResponseDTO user = authApi.getById(userId);
            ProfileResponseDTO profile = user.profile();
            if (profile != null) {
                String name = String.join(" ",
                        profile.firstName() == null ? "" : profile.firstName(),
                        profile.lastName() == null ? "" : profile.lastName()).strip();
                if (!name.isEmpty()) {
                    return name;
                }
            }
            return user.username();
        } catch (UserException e) {
            return null;
        }
    }

    private Participants requireParticipant(Long conversationId) {
        Long me = requireUser();
        List<Participants> found = jdbc.query("""
                        SELECT client_id, agent_id FROM chat_conversations
                        WHERE id = :id AND (client_id = :me OR agent_id = :me)""",
                new MapSqlParameterSource().addValue("id", conversationId).addValue("me", me),
                (rs, i) -> new Participants(me, rs.getLong("client_id"), rs.getLong("agent_id")));
        if (found.isEmpty()) {
            throw new UserException("Conversation not found");
        }
        return found.getFirst();
    }

    private void publishAfterCommit(ChatEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eventPublisher.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(event);
            }
        });
    }

    private static ChatMessageDTO toMessage(ResultSet rs, Participants p) throws SQLException {
        return new ChatMessageDTO(
                rs.getLong("id"),
                rs.getLong("conversation_id"),
                rs.getLong("sender_id") == p.clientId() ? ChatRole.CLIENT : ChatRole.AGENT,
                rs.getString("body"),
                toInstant(rs.getTimestamp("created_at")));
    }

    private static Instant toInstant(Object value) {
        return value instanceof Timestamp ts ? ts.toInstant() : null;
    }

    private static String readColumn(ChatRole role) {
        return role == ChatRole.CLIENT ? "client_read_id" : "agent_read_id";
    }

    private static Long requireUser() {
        Long me = UserContext.getUserId();
        if (me == null) {
            throw new UserException("Sign in to use chat");
        }
        return me;
    }

    private record Participants(Long me, long clientId, long agentId) {
        ChatRole myRole() {
            return me == clientId ? ChatRole.CLIENT : ChatRole.AGENT;
        }
    }
}
