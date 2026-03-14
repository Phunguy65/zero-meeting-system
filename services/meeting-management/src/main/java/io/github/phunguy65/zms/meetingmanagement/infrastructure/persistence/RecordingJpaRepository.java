package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import io.github.phunguy65.zms.meetingmanagement.domain.model.RecordingStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecordingJpaRepository extends JpaRepository<RecordingJpaEntity, UUID> {

    List<RecordingJpaEntity> findByMeetingId(UUID meetingId);

    @Query(
            "SELECT r FROM RecordingJpaEntity r WHERE r.meetingId = :meetingId AND r.status IN :statuses")
    Optional<RecordingJpaEntity> findByMeetingIdAndStatusIn(
            @Param("meetingId") UUID meetingId, @Param("statuses") List<RecordingStatus> statuses);
}
