package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingJpaRepository extends JpaRepository<MeetingJpaEntity, UUID> {

    Optional<MeetingJpaEntity> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    /**
     * Keyset-scroll query for meetings by host, ordered by (created_at DESC, id DESC).
     * Caller should request {@code size + 1} rows to detect next page.
     */
    @Query(
            value = "SELECT * FROM meetings m WHERE m.host_id = CAST(:hostId AS uuid) "
                    + "AND (CAST(:cursorCreatedAt AS timestamptz) IS NULL OR (m.created_at, m.id) < (CAST(:cursorCreatedAt AS timestamptz), CAST(:cursorId AS uuid))) "
                    + "ORDER BY m.created_at DESC, m.id DESC "
                    + "LIMIT :limit",
            nativeQuery = true)
    List<MeetingJpaEntity> findByHostIdKeyset(
            @Param("hostId") String hostId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") String cursorId,
            @Param("limit") int limit);
}
