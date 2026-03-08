package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.infrastructure.web.SliceHttpResponse;
import io.github.phunguy65.zms.usermanagement.application.dto.UserResponse;
import io.github.phunguy65.zms.usermanagement.application.service.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.port.UserFilter;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetUsersSliceUseCase {

    private final UserRepository userRepository;
    private final UserPreferencesParser preferencesParser;

    public GetUsersSliceUseCase(
            UserRepository userRepository, UserPreferencesParser preferencesParser) {
        this.userRepository = userRepository;
        this.preferencesParser = preferencesParser;
    }

    @Transactional(readOnly = true)
    public Result<SliceHttpResponse<UserResponse>, AuthErrorCode> execute(
            int page, int size, UserFilter filter) {
        var pageResult = userRepository.findActiveUsers(page, size, filter);
        var items = pageResult.items().stream().map(this::toResponse).toList();
        var response = new SliceHttpResponse<>(
                items,
                pageResult.pageNumber(),
                pageResult.pageSize(),
                pageResult.hasNext(),
                pageResult.hasPrevious());
        return Result.success(response);
    }

    private UserResponse toResponse(User user) {
        var prefs = preferencesParser.parseAsResponse(user.getPreferences());
        return new UserResponse(
                user.getId(),
                user.getEmail().value(),
                user.getFullName().value(),
                user.getAvatarUrl().orElse(null),
                user.getAuthProvider(),
                prefs,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
