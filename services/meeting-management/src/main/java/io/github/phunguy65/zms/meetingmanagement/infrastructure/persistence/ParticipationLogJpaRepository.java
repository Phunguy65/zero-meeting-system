package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipationLogJpaRepository
        extends JpaRepository<ParticipationLogJpaEntity, Long> {

    List<ParticipationLogJpaEntity> findByMeetingId(UUID meetingId);

    @Query("SELECT COUNT(p) FROM ParticipationLogJpaEntity p "
            + "WHERE p.meetingId = :meetingId AND p.leftAt IS NULL")
    long countActiveByMeetingId(@Param("meetingId") UUID meetingId);

    /**
     * Primary lookup for participant_left webhook: find active session by LiveKit SID.
     */
    @Query("SELECT p FROM ParticipationLogJpaEntity p "
            + "WHERE p.livekitParticipantSid = :sid AND p.leftAt IS NULL")
    Optional<ParticipationLogJpaEntity> findActiveBySid(@Param("sid") String sid);

    /**
     * Lookup for participant_joined webhook: find pending entry to assign SID.
     */
    @Query("SELECT p FROM ParticipationLogJpaEntity p "
            + "WHERE p.meetingId = :meetingId AND p.livekitIdentity = :identity AND p.leftAt IS NULL "
            + "ORDER BY p.joinedAt DESC")
    Optional<ParticipationLogJpaEntity> findActiveByMeetingIdAndIdentity(
            @Param("meetingId") UUID meetingId, @Param("identity") String identity);

    /**
     * Bulk lookup for room_finished / EndMeeting: all active sessions for a meeting.
     */
    @Query("SELECT p FROM ParticipationLogJpaEntity p "
            + "WHERE p.meetingId = :meetingId AND p.leftAt IS NULL")
    List<ParticipationLogJpaEntity> findActiveByMeetingId(@Param("meetingId") UUID meetingId);

    /**
     * Keyset-scroll query for participation logs by meeting, ordered by (joined_at DESC, id DESC).
     * Caller should request {@code size + 1} rows to detect next page.
     */
    @Query(
            value =
                    "SELECT * FROM participation_logs p WHERE p.meeting_id = CAST(:meetingId AS uuid) "
                            + "AND (:cursorJoinedAt IS NULL OR (p.joined_at, p.id) < (CAST(:cursorJoinedAt AS timestamptz), :cursorId)) "
                            + "ORDER BY p.joined_at DESC, p.id DESC "
                            + "LIMIT :limit",
            nativeQuery = true)
    List<ParticipationLogJpaEntity> findByMeetingIdKeyset(
            @Param("meetingId") String meetingId,
            @Param("cursorJoinedAt") Instant cursorJoinedAt,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit);
}
