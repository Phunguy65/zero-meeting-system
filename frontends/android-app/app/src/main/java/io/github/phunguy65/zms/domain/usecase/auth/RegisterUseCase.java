package io.github.phunguy65.zms.domain.usecase.auth;

import io.github.phunguy65.zms.domain.model.RegisterResult;
import io.github.phunguy65.zms.domain.repository.AuthRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/** Use case for new user registration. */
public class RegisterUseCase {

    private final AuthRepository authRepository;

    @Inject
    public RegisterUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    /**
     * Executes the registration operation asynchronously.
     *
     * @param fullName user's display name
     * @param username chosen username
     * @param email email address
     * @param password chosen password
     * @return a future that completes with registration result containing server-assigned user data
     */
    public CompletableFuture<RegisterResult> execute(
            String fullName, String username, String email, String password) {
        return authRepository.register(fullName, username, email, password);
    }
}
