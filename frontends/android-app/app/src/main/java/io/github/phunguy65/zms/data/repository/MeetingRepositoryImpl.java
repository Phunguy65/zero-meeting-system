package io.github.phunguy65.zms.data.repository;

import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import javax.inject.Inject;

/** Implementation of {@link MeetingRepository} backed by remote API. */
public class MeetingRepositoryImpl implements MeetingRepository {

    @Inject
    public MeetingRepositoryImpl() {}
}
