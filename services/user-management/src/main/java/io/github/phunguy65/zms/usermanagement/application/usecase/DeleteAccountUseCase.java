package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.DeleteAccountResponse;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.port.RefreshTokenRepository;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteAccountUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public DeleteAccountUseCase(
            UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public Result<DeleteAccountResponse, AuthErrorCode> execute(UUID userId) {
        var userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            return Result.failure(AuthErrorCode.USER_NOT_FOUND);
        }

        var user = userOpt.get();
        if (user.isDeleted()) {
            return Result.success(new DeleteAccountResponse(
                    user.getId(),
                    user.getEmail().value(),
                    user.getFullName().value(),
                    user.getDeletedAt()));
        }

        user.delete();
        var saved = userRepository.save(user);
        refreshTokenRepository.revokeAllByUserId(userId);

        return Result.success(new DeleteAccountResponse(
                saved.getId(),
                saved.getEmail().value(),
                saved.getFullName().value(),
                saved.getDeletedAt()));
    }
}
