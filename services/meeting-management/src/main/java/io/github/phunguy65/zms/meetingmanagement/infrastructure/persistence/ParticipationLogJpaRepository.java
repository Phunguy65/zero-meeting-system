package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipationLogJpaRepository
        extends JpaRepository<ParticipationLogJpaEntity, Long> {

    List<ParticipationLogJpaEntity> findByMeetingId(UUID meetingId);

    /**
     * Finds the most recent active (not yet left) session for a given meeting and device.
     */
    @Query("SELECT p FROM ParticipationLogJpaEntity p "
            + "WHERE p.meetingId = :meetingId AND p.deviceId = :deviceId AND p.leftAt IS NULL "
            + "ORDER BY p.joinedAt DESC")
    Optional<ParticipationLogJpaEntity> findActiveByMeetingIdAndDeviceId(
            @Param("meetingId") UUID meetingId, @Param("deviceId") String deviceId);
}
