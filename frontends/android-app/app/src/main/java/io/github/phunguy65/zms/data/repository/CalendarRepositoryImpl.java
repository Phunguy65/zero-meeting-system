package io.github.phunguy65.zms.data.repository;

import io.github.phunguy65.zms.domain.repository.CalendarRepository;
import javax.inject.Inject;

/** Implementation of {@link CalendarRepository} backed by remote API. */
public class CalendarRepositoryImpl implements CalendarRepository {

    @Inject
    public CalendarRepositoryImpl() {}
}
