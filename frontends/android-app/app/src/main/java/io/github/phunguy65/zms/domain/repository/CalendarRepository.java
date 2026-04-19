package io.github.phunguy65.zms.domain.repository;

import io.github.phunguy65.zms.domain.model.CalendarEvent;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Repository interface for calendar event operations. */
public interface CalendarRepository {

    /**
     * Retrieves calendar events within the specified date range.
     *
     * <p>Fetches host meetings from the API, filters by date range, and maps to CalendarEvent.
     *
     * @param start the start of the date range (inclusive)
     * @param end the end of the date range (inclusive)
     * @return a CompletableFuture that completes with the list of calendar events
     */
    CompletableFuture<List<CalendarEvent>> getEventsForDateRange(OffsetDateTime start, OffsetDateTime end);
}
