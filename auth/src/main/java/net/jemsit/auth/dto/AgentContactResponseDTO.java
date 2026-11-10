package net.jemsit.auth.dto;

/**
 * The public contact details of a listing's agent; only what visitors are meant to see.
 * avatarUrl is absolute (the public media host), or null when the agent has no photo.
 */
public record AgentContactResponseDTO(String telegramUsername, String avatarUrl) {
}
