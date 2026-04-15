package io.github.phunguy65.zms.domain.usecase.me;

import io.github.phunguy65.zms.domain.model.User;
import io.github.phunguy65.zms.domain.repository.MeRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * Use case for fetching the current authenticated user's profile.
 *
 * <p>Called after successful login (when rememberMe is enabled) to fetch
 * user profile data for local persistence.
 */
public class GetMeUseCase {

    private final MeRepository meRepository;

    @Inject
    public GetMeUseCase(MeRepository meRepository) {
        this.meRepository = meRepository;
    }

    /**
     * Executes the get current user operation asynchronously.
     *
     * @return a future that completes with the user profile
     */
    public CompletableFuture<User> execute() {
        return meRepository.getMe();
    }
}
