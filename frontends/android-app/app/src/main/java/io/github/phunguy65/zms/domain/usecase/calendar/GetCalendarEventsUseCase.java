package io.github.phunguy65.zms.domain.usecase.calendar;

import io.github.phunguy65.zms.domain.repository.CalendarRepository;
import javax.inject.Inject;

/** Use case for retrieving calendar events. */
public class GetCalendarEventsUseCase {

    private final CalendarRepository calendarRepository;

    @Inject
    public GetCalendarEventsUseCase(CalendarRepository calendarRepository) {
        this.calendarRepository = calendarRepository;
    }
}
