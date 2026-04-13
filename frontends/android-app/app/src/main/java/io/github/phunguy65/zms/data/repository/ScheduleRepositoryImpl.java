package io.github.phunguy65.zms.data.repository;

import io.github.phunguy65.zms.domain.repository.ScheduleRepository;
import javax.inject.Inject;

/** Implementation of {@link ScheduleRepository} backed by remote API. */
public class ScheduleRepositoryImpl implements ScheduleRepository {

    @Inject
    public ScheduleRepositoryImpl() {}
}
