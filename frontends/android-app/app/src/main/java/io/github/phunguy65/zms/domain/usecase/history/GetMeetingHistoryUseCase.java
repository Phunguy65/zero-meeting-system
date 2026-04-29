package io.github.phunguy65.zms.domain.usecase.history;

import io.github.phunguy65.zms.domain.model.MeetingHistoryPage;
import io.github.phunguy65.zms.domain.repository.MeetingHistoryRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/** Use case for loading the authenticated user's meeting history. */
public class GetMeetingHistoryUseCase {

    private final MeetingHistoryRepository meetingHistoryRepository;

    @Inject
    public GetMeetingHistoryUseCase(MeetingHistoryRepository meetingHistoryRepository) {
        this.meetingHistoryRepository = meetingHistoryRepository;
    }

    public CompletableFuture<MeetingHistoryPage> execute(
            String userId, int pageSize, String pageToken) {
        return meetingHistoryRepository.getMeetingHistory(userId, pageSize, pageToken);
    }
}
