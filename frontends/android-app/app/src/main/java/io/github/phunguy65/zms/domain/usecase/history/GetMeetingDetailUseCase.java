package io.github.phunguy65.zms.domain.usecase.history;

import io.github.phunguy65.zms.domain.model.MeetingHistoryDetail;
import io.github.phunguy65.zms.domain.repository.MeetingHistoryRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/** Use case for loading a specific historical meeting detail. */
public class GetMeetingDetailUseCase {

    private final MeetingHistoryRepository meetingHistoryRepository;

    @Inject
    public GetMeetingDetailUseCase(MeetingHistoryRepository meetingHistoryRepository) {
        this.meetingHistoryRepository = meetingHistoryRepository;
    }

    public CompletableFuture<MeetingHistoryDetail> execute(String userId, String meetingId) {
        return meetingHistoryRepository.getMeetingDetail(userId, meetingId);
    }
}
