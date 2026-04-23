package io.github.phunguy65.zms.domain.usecase.me;

import io.github.phunguy65.zms.domain.repository.MeRepository;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * Use case for permanently deleting the current user's account.
 *
 * <p>Orchestrates remote account deletion via {@link MeRepository#deleteMe()} followed by
 * local session cleanup via {@link SessionRepository#clearAllSessionData()} on success.
 */
public class DeleteAccountUseCase {

    private final MeRepository meRepository;
    private final SessionRepository sessionRepository;

    @Inject
    public DeleteAccountUseCase(
            MeRepository meRepository, SessionRepository sessionRepository) {
        this.meRepository = meRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Executes the account deletion workflow.
     *
     * <p>Calls the remote delete endpoint first. On success, clears all locally stored
     * session data (tokens, session info, remember-me flag).
     *
     * @return a future that completes with {@link Void} after both remote deletion and local
     *     cleanup succeed
     */
    public CompletableFuture<Void> execute() {
        return meRepository
                .deleteMe()
                .thenRun(sessionRepository::clearAllSessionData);
    }
}
