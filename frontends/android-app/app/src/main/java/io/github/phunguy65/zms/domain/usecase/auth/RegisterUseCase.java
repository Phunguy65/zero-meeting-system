package io.github.phunguy65.zms.domain.usecase.auth;

import io.github.phunguy65.zms.domain.repository.AuthRepository;
import javax.inject.Inject;

/** Use case for new user registration. */
public class RegisterUseCase {

    private final AuthRepository authRepository;

    @Inject
    public RegisterUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }
}
