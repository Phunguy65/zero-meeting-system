package io.github.phunguy65.zms.domain.usecase.schedule;

import io.github.phunguy65.zms.domain.repository.ScheduleRepository;
import javax.inject.Inject;

/** Use case for retrieving scheduled meetings. */
public class GetScheduleUseCase {

    private final ScheduleRepository scheduleRepository;

    @Inject
    public GetScheduleUseCase(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }
}
