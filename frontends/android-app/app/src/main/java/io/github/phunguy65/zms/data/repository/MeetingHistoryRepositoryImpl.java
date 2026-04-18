package io.github.phunguy65.zms.data.repository;

import io.github.phunguy65.zms.data.mapper.MeetingMapper;
import io.github.phunguy65.zms.data.remote.api.UserMeetingsApi;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementCursorScrollResponseMeetingResponse;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementMeetingDetailResponse;
import io.github.phunguy65.zms.di.IoExecutor;
import io.github.phunguy65.zms.domain.model.MeetingHistory;
import io.github.phunguy65.zms.domain.model.MeetingHistoryDetail;
import io.github.phunguy65.zms.domain.model.MeetingHistoryPage;
import io.github.phunguy65.zms.domain.repository.MeetingHistoryRepository;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;
import retrofit2.Response;

/** Implementation of {@link MeetingHistoryRepository} backed by the user meetings API. */
public class MeetingHistoryRepositoryImpl implements MeetingHistoryRepository {

    private static final String HISTORY_STATUS_FILTER = "ENDED,CANCELLED";

    private final UserMeetingsApi userMeetingsApi;
    private final MeetingMapper meetingMapper;
    private final Executor ioExecutor;

    @Inject
    public MeetingHistoryRepositoryImpl(
            UserMeetingsApi userMeetingsApi,
            MeetingMapper meetingMapper,
            @IoExecutor Executor ioExecutor) {
        this.userMeetingsApi = userMeetingsApi;
        this.meetingMapper = meetingMapper;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public CompletableFuture<MeetingHistoryPage> getMeetingHistory(
            String userId, int pageSize, String pageToken) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        Response<MeetingManagementCursorScrollResponseMeetingResponse> response =
                                userMeetingsApi
                                        .listParticipatedMeetings(
                                                UUID.fromString(userId),
                                                pageSize,
                                                pageToken,
                                                HISTORY_STATUS_FILTER)
                                        .execute();

                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException(
                                    "Get meeting history failed: HTTP " + response.code());
                        }

                        MeetingManagementCursorScrollResponseMeetingResponse body = response.body();
                        List<MeetingHistory> items =
                                body.getContent() == null
                                        ? List.of()
                                        : body.getContent().stream()
                                                .map(meetingMapper::toMeetingHistory)
                                                .collect(Collectors.toList());

                        String nextToken = body.getNextPageToken();
                        return new MeetingHistoryPage(items, nextToken, nextToken != null);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                },
                ioExecutor);
    }

    @Override
    public CompletableFuture<MeetingHistoryDetail> getMeetingDetail(String userId, String meetingId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        Response<MeetingManagementMeetingDetailResponse> response =
                                userMeetingsApi
                                        .getParticipatedMeetingDetail(
                                                UUID.fromString(userId), UUID.fromString(meetingId))
                                        .execute();

                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException(
                                    "Get meeting detail failed: HTTP " + response.code());
                        }

                        return meetingMapper.toMeetingHistoryDetail(response.body());
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                },
                ioExecutor);
    }
}
