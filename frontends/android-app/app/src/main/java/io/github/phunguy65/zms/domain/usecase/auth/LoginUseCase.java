package io.github.phunguy65.zms.domain.usecase.auth;

import io.github.phunguy65.zms.domain.repository.AuthRepository;
import javax.inject.Inject;

/** Use case for user login via email and password. */
public class LoginUseCase {

    private final AuthRepository authRepository;

    @Inject
    public LoginUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }
}
