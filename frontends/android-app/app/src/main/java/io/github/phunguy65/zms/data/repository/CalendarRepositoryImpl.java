package io.github.phunguy65.zms.data.repository;

import io.github.phunguy65.zms.data.mapper.MeetingMapper;
import io.github.phunguy65.zms.data.remote.api.MeetingsApi;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementCursorScrollResponseMeetingResponse;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementMeetingResponse;
import io.github.phunguy65.zms.di.IoExecutor;
import io.github.phunguy65.zms.domain.model.CalendarEvent;
import io.github.phunguy65.zms.domain.repository.CalendarRepository;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;
import retrofit2.Response;

/** Implementation of {@link CalendarRepository} backed by remote API. */
public class CalendarRepositoryImpl implements CalendarRepository {

    private static final int CALENDAR_PAGE_SIZE = 100;

    private final MeetingsApi meetingsApi;
    private final MeetingMapper meetingMapper;
    private final Executor ioExecutor;

    @Inject
    public CalendarRepositoryImpl(
            MeetingsApi meetingsApi,
            MeetingMapper meetingMapper,
            @IoExecutor Executor ioExecutor) {
        this.meetingsApi = meetingsApi;
        this.meetingMapper = meetingMapper;
        this.ioExecutor = ioExecutor;
    }

    @Override
    public CompletableFuture<List<CalendarEvent>> getEventsForDateRange(
            OffsetDateTime start, OffsetDateTime end) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        Response<MeetingManagementCursorScrollResponseMeetingResponse> response =
                                meetingsApi.listHostMeetings(CALENDAR_PAGE_SIZE, null).execute();

                        if (!response.isSuccessful() || response.body() == null) {
                            throw new IOException(
                                    "List host meetings failed: HTTP " + response.code());
                        }

                        List<MeetingManagementMeetingResponse> content = response.body().getContent();
                        if (content == null) {
                            return List.of();
                        }

                        return content.stream()
                                .filter(m -> isInDateRange(m, start, end))
                                .map(meetingMapper::toCalendarEvent)
                                .collect(Collectors.toList());
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                },
                ioExecutor);
    }

    /**
     * Checks if a meeting overlaps with the specified date range.
     *
     * <p>A meeting is considered in range if:
     * - Its startTime is before the range end AND
     * - Its endTime (or startTime if no endTime) is after the range start
     */
    private boolean isInDateRange(
            MeetingManagementMeetingResponse meeting, OffsetDateTime start, OffsetDateTime end) {
        OffsetDateTime meetingStart = meeting.getStartTime();
        OffsetDateTime meetingEnd = meeting.getEndTime();

        if (meetingStart == null) {
            return false;
        }

        if (meetingEnd == null) {
            meetingEnd = meetingStart;
        }

        return meetingStart.isBefore(end) && meetingEnd.isAfter(start);
    }
}
