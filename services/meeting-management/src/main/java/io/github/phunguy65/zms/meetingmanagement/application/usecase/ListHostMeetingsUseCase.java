package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.query.ListHostMeetingsQuery;
import io.github.phunguy65.zms.meetingmanagement.application.response.MeetingResponse;
import io.github.phunguy65.zms.meetingmanagement.application.response.MeetingSettingsResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.model.Meeting;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingTitle;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.shared.domain.CursorPageResponse;
import io.github.phunguy65.zms.shared.domain.ScrollCursor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListHostMeetingsUseCase {

    private final MeetingRepository meetingRepository;

    public ListHostMeetingsUseCase(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<MeetingResponse> execute(
            ListHostMeetingsQuery query, ScrollCursor cursor) {
        var page = meetingRepository.findByHostId(query.hostId(), query);
        var items = page.items().stream().map(this::toResponse).toList();
        return new CursorPageResponse<>(items, page.pageSize(), page.hasNext());
    }

    private MeetingResponse toResponse(Meeting m) {
        return new MeetingResponse(
                m.getId().value(),
                m.getHostId().value(),
                m.getShortCode().value(),
                m.getTitle().map(MeetingTitle::value).orElse(null),
                m.getDescription().orElse(null),
                m.getStartTime().orElse(null),
                m.getEndTime().orElse(null),
                m.getType(),
                m.getStatus(),
                MeetingSettingsResponse.from(m.getSettings()),
                m.getCreatedAt());
    }
}
