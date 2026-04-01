package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import io.github.phunguy65.zms.meetingmanagement.domain.projection.ParticipantSummary;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipationLogJpaRepository
        extends JpaRepository<ParticipationLogJpaEntity, Long> {
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

    @Query("SELECT p FROM ParticipationLogJpaEntity p "
            + "WHERE p.userId = :userId AND p.leftAt IS NULL")
    List<ParticipationLogJpaEntity> findActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT new io.github.phunguy65.zms.meetingmanagement.domain.projection"
            + ".ParticipantSummaryProjection("
            + "p.id, p.meetingId, p.userId, p.displayName, p.role, "
            + "p.joinedAt, p.leftAt) "
            + "FROM ParticipationLogJpaEntity p "
            + "WHERE p.meetingId = :meetingId "
            + "ORDER BY p.joinedAt DESC, p.id DESC")
    List<ParticipantSummary> findParticipantSummariesByMeetingId(
            @Param("meetingId") UUID meetingId);
}
