package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.UserResponse;
import io.github.phunguy65.zms.usermanagement.application.service.UserResponseMapper;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUserUseCase {

    private final UserRepository userRepository;
    private final UserResponseMapper userResponseMapper;

    public GetUserUseCase(UserRepository userRepository, UserResponseMapper userResponseMapper) {
        this.userRepository = userRepository;
        this.userResponseMapper = userResponseMapper;
    }

    @Transactional(readOnly = true)
    public Result<UserResponse, AuthErrorCode> execute(UUID userId) {
        return userRepository
                .findActiveById(userId)
                .map(user -> Result.<UserResponse, AuthErrorCode>success(
                        userResponseMapper.toResponse(user)))
                .orElseGet(() -> Result.failure(AuthErrorCode.USER_NOT_FOUND));
    }
}
