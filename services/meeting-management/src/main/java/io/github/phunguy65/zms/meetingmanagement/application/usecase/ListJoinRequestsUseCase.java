package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.response.JoinRequestResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequest;
import io.github.phunguy65.zms.meetingmanagement.domain.port.JoinRequestRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListJoinRequestsUseCase {

    private final MeetingRepository meetingRepository;
    private final JoinRequestRepository joinRequestRepository;

    public ListJoinRequestsUseCase(
            MeetingRepository meetingRepository, JoinRequestRepository joinRequestRepository) {
        this.meetingRepository = meetingRepository;
        this.joinRequestRepository = joinRequestRepository;
    }

    @Transactional(readOnly = true)
    public Result<List<JoinRequestResponse>, MeetingError> execute(
            UUID meetingId, UUID requesterId) {
        var meetingOpt = meetingRepository.findById(meetingId);
        if (meetingOpt.isEmpty()) {
            return Result.failure(new MeetingError.MeetingNotFound(meetingId));
        }
        var meeting = meetingOpt.get();

        // Validate host
        if (!meeting.getHostId().equals(UserId.of(requesterId))) {
            return Result.failure(new MeetingError.NotAuthorized(
                    requesterId, meeting.getHostId().value()));
        }

        List<JoinRequestResponse> responses =
                joinRequestRepository.findPendingByMeetingId(meetingId).stream()
                        .sorted(Comparator.comparing(JoinRequest::getRequestedAt))
                        .map(r -> new JoinRequestResponse(
                                r.getId().value(),
                                r.getMeetingId().value(),
                                r.getUserId().map(UserId::value).orElse(null),
                                r.getDisplayName(),
                                r.getStatus(),
                                r.getRequestedAt(),
                                r.getExpiresAt()))
                        .toList();

        return Result.success(responses);
    }
}
