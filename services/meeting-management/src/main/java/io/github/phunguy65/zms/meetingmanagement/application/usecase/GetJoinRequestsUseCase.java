package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.query.GetJoinRequestsQuery;
import io.github.phunguy65.zms.meetingmanagement.application.response.JoinRequestResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.port.JoinRequestRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.projection.JoinRequestSummary;
import io.github.phunguy65.zms.shared.domain.OffsetPageResponse;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetJoinRequestsUseCase {

    private final MeetingRepository meetingRepository;
    private final JoinRequestRepository joinRequestRepository;

    public GetJoinRequestsUseCase(
            MeetingRepository meetingRepository, JoinRequestRepository joinRequestRepository) {
        this.meetingRepository = meetingRepository;
        this.joinRequestRepository = joinRequestRepository;
    }

    @Transactional(readOnly = true)
    public Result<OffsetPageResponse<JoinRequestResponse>, MeetingError> execute(
            GetJoinRequestsQuery query) {
        var meetingOpt = meetingRepository.findById(query.meetingId());
        if (meetingOpt.isEmpty()) {
            return Result.failure(new MeetingError.MeetingNotFound(query.meetingId()));
        }
        var meeting = meetingOpt.get();

        if (!meeting.getHostId().equals(UserId.of(query.requesterId()))) {
            return Result.failure(new MeetingError.NotAuthorized(
                    query.requesterId(), meeting.getHostId().value()));
        }

        var page = joinRequestRepository.findPendingSummariesByMeetingId(
                query.meetingId(), query.offset(), query.pageSize());
        List<JoinRequestResponse> responses =
                page.items().stream().map(this::toResponse).toList();

        return Result.success(
                OffsetPageResponse.of(responses, page.pageSize(), page.offset(), page.hasNext()));
    }

    private JoinRequestResponse toResponse(JoinRequestSummary request) {
        return new JoinRequestResponse(
                request.id(),
                request.meetingId(),
                request.userId(),
                request.displayName(),
                request.status(),
                request.requestedAt(),
                request.expiresAt());
    }
}
