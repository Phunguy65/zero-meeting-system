package io.github.phunguy65.zms.domain.repository;

import io.github.phunguy65.zms.domain.model.ParticipantRoleInfo;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Repository contract for fetching participant role metadata from the backend.
 */
public interface ParticipantRepository {

    /**
     * Fetches the list of participant role entries for a meeting.
     */
    CompletableFuture<List<ParticipantRoleInfo>> getParticipantRoles(String meetingId);

    /**
     * Mutes the microphone of all active participants in a meeting.
     */
    CompletableFuture<Void> muteAll(String meetingId);

    /**
     * Mutes a specific participant's track (microphone or camera) in a meeting.
     */
    CompletableFuture<Void> muteTrack(String meetingId, String identity, String source);
}
