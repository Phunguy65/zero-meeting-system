package io.github.phunguy65.zms.data.repository;

import io.github.phunguy65.zms.domain.repository.ProfileRepository;
import javax.inject.Inject;

/** Implementation of {@link ProfileRepository} backed by remote API. */
public class ProfileRepositoryImpl implements ProfileRepository {

    @Inject
    public ProfileRepositoryImpl() {}
}
