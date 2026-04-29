package io.github.phunguy65.zms.domain.usecase.calendar;

import io.github.phunguy65.zms.domain.model.CalendarEvent;
import io.github.phunguy65.zms.domain.repository.CalendarRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/** Use case for retrieving calendar events within a date range. */
public class GetCalendarEventsUseCase {

    private final CalendarRepository calendarRepository;

    @Inject
    public GetCalendarEventsUseCase(CalendarRepository calendarRepository) {
        this.calendarRepository = calendarRepository;
    }

    /**
     * Retrieves calendar events within the specified date range.
     *
     * @param start the start of the date range (inclusive)
     * @param end the end of the date range (inclusive)
     * @return a CompletableFuture that completes with the list of calendar events
     */
    public CompletableFuture<List<CalendarEvent>> execute(
            OffsetDateTime start, OffsetDateTime end) {
        return calendarRepository.getEventsForDateRange(start, end);
    }
}
