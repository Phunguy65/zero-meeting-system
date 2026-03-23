package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.query.GetParticipantsQuery;
import io.github.phunguy65.zms.meetingmanagement.application.response.ParticipantResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ParticipationLogCursor;
import io.github.phunguy65.zms.meetingmanagement.domain.port.ParticipationLogRepository;
import io.github.phunguy65.zms.shared.domain.CursorPageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetParticipantsUseCase {

    private final ParticipationLogRepository participationLogRepository;

    public GetParticipantsUseCase(ParticipationLogRepository participationLogRepository) {
        this.participationLogRepository = participationLogRepository;
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<ParticipantResponse> execute(
            GetParticipantsQuery query, ParticipationLogCursor cursor) {
        var page = participationLogRepository.findByMeetingIdKeyset(
                query.meetingId(), cursor, query.pageSize());
        var items = page.items().stream()
                .map(log -> new ParticipantResponse(
                        log.getId().value(),
                        log.getMeetingId().value(),
                        log.getUserId().orElse(null),
                        log.getDisplayName(),
                        log.getRole(),
                        log.getJoinedAt(),
                        log.getLeftAt().orElse(null)))
                .toList();
        return new CursorPageResponse<>(items, page.pageSize(), page.hasNext());
    }
}
