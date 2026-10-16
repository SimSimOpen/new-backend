package net.jemsit.common.dto.response.dashboard;

import java.time.LocalDateTime;

/**
 * One entry of the admin dashboard's activity feed. {@code type} is a stable code the frontend
 * translates (e.g. LISTING_CREATED, USER_REGISTERED); {@code title} and {@code actor} are the
 * listing title / account name it refers to, and {@code detail} an optional extra such as a status.
 */
public record ActivityItemDTO(String type, Long refId, String title, String actor, String detail, LocalDateTime at) {
}
