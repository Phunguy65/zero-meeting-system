package io.github.phunguy65.zms.data.repository;

import io.github.phunguy65.zms.domain.repository.AuthRepository;
import javax.inject.Inject;

/** Implementation of {@link AuthRepository} backed by remote API. */
public class AuthRepositoryImpl implements AuthRepository {

    @Inject
    public AuthRepositoryImpl() {}
}
