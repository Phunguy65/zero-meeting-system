package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import io.github.phunguy65.zms.meetingmanagement.domain.model.InviteeStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingInvitee;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.InviteeDisplayName;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.InviteeId;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.InviterId;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingInviteeRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.projection.InviteeSummary;
import io.github.phunguy65.zms.shared.domain.valueobject.Email;
import io.github.phunguy65.zms.shared.domain.valueobject.MeetingId;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MeetingInviteeRepositoryAdapter implements MeetingInviteeRepository {

    private final MeetingInviteeJpaRepository jpa;

    public MeetingInviteeRepositoryAdapter(MeetingInviteeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<MeetingInvitee> saveAll(List<MeetingInvitee> invitees) {
        List<MeetingInviteeJpaEntity> entities =
                invitees.stream().map(this::toEntity).toList();
        jpa.saveAll(entities);
        return invitees;
    }

    @Override
    public MeetingInvitee save(MeetingInvitee invitee) {
        jpa.save(toEntity(invitee));
        return invitee;
    }

    @Override
    public Optional<MeetingInvitee> findById(InviteeId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<MeetingInvitee> findByMeetingId(UUID meetingId) {
        return jpa.findByMeetingId(meetingId).stream().map(this::toDomain).toList();
    }

    @Override
    public long countActiveByMeetingId(UUID meetingId) {
        return jpa.countByMeetingIdAndStatusIn(
                meetingId, List.of(InviteeStatus.PENDING.name(), InviteeStatus.ACCEPTED.name()));
    }

    @Override
    public List<InviteeSummary> findSummariesByMeetingId(UUID meetingId) {
        return jpa.findSummariesByMeetingId(meetingId);
    }

    private MeetingInvitee toDomain(MeetingInviteeJpaEntity e) {
        return MeetingInvitee.reconstitute(
                InviteeId.of(e.getId()),
                MeetingId.of(e.getMeetingId()),
                InviterId.of(e.getInviterId()),
                e.getUserId() != null ? UserId.of(e.getUserId()) : null,
                Email.of(e.getEmail()),
                e.getDisplayName() != null ? InviteeDisplayName.of(e.getDisplayName()) : null,
                InviteeStatus.valueOf(e.getStatus()),
                e.getInvitedAt(),
                e.getRespondedAt());
    }

    private MeetingInviteeJpaEntity toEntity(MeetingInvitee i) {
        return new MeetingInviteeJpaEntity(
                i.getId().value(),
                i.getMeetingId().value(),
                i.getInviterId().value(),
                i.getUserId().map(UserId::value).orElse(null),
                i.getEmail().value(),
                i.getDisplayName().map(InviteeDisplayName::value).orElse(null),
                i.getStatus().name(),
                i.getInvitedAt(),
                i.getRespondedAt().orElse(null));
    }
}
