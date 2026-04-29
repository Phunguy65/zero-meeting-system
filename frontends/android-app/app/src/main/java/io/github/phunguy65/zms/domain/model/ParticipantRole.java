package io.github.phunguy65.zms.domain.model;

/**
 * Participant role within a meeting context.
 *
 * <ul>
 *   <li>HOST — meeting owner with full moderation rights.</li>
 *   <li>PARTICIPANT — authenticated attendee with default rights.</li>
 *   <li>GUEST — unauthenticated attendee with restricted rights.</li>
 * </ul>
 */
public enum ParticipantRole {
    HOST,
    PARTICIPANT,
    GUEST
}
