package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "participation_logs")
public class ParticipationLogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_id", nullable = false, columnDefinition = "uuid")
    private UUID meetingId;

    @Column(name = "user_id", columnDefinition = "uuid")
    private @Nullable UUID userId;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private @Nullable Instant leftAt;

    @Column(name = "device_id", length = 255)
    private @Nullable String deviceId;

    protected ParticipationLogJpaEntity() {}

    public ParticipationLogJpaEntity(
            UUID meetingId,
            @Nullable UUID userId,
            String displayName,
            ParticipantRole role,
            Instant joinedAt,
            @Nullable Instant leftAt,
            @Nullable String deviceId) {
        this.meetingId = meetingId;
        this.userId = userId;
        this.displayName = displayName;
        this.role = role;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
        this.deviceId = deviceId;
    }

    public Long getId() {
        return id;
    }

    public UUID getMeetingId() {
        return meetingId;
    }

    public @Nullable UUID getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ParticipantRole getRole() {
        return role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public @Nullable Instant getLeftAt() {
        return leftAt;
    }

    public @Nullable String getDeviceId() {
        return deviceId;
    }

    /** Called by the adapter to update left_at after a participant leaves. */
    public void setLeftAt(Instant leftAt) {
        this.leftAt = leftAt;
    }
}
