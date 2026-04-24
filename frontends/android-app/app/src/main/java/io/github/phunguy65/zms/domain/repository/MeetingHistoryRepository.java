package io.github.phunguy65.zms.domain.repository;

import io.github.phunguy65.zms.domain.model.MeetingHistoryDetail;
import io.github.phunguy65.zms.domain.model.MeetingHistoryPage;
import java.util.concurrent.CompletableFuture;

/** Repository for meeting history list and detail queries. */
public interface MeetingHistoryRepository {

    CompletableFuture<MeetingHistoryPage> getMeetingHistory(
            String userId, int pageSize, String pageToken);

    CompletableFuture<MeetingHistoryDetail> getMeetingDetail(String userId, String meetingId);
}
