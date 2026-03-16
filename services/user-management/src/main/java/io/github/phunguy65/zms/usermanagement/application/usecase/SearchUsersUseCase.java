package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.CursorPageResponse;
import io.github.phunguy65.zms.shared.domain.ScrollCursor;
import io.github.phunguy65.zms.usermanagement.application.helper.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.application.query.SearchUsersQuery;
import io.github.phunguy65.zms.usermanagement.application.response.UserResponse;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.model.Username;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import io.github.phunguy65.zms.usermanagement.domain.port.UserScrollFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchUsersUseCase {

    private final UserRepository userRepository;
    private final UserPreferencesParser preferencesParser;

    public SearchUsersUseCase(
            UserRepository userRepository, UserPreferencesParser preferencesParser) {
        this.userRepository = userRepository;
        this.preferencesParser = preferencesParser;
    }

    /**
     * Searches users by optional query string with keyset pagination.
     *
     * <p>Cursor decoding is delegated to the caller (controller) via {@link CursorTokenEncoder}.
     * The returned {@link CursorPageResponse} carries the raw domain items and a flag indicating
     * whether more pages exist; the controller is responsible for encoding the next page token and
     * wrapping the result in the HTTP response envelope.
     *
     * @param query  pagination and filter parameters
     * @param cursor decoded cursor from the previous page, or {@code null} for the first page
     * @return a page of {@link UserResponse} items
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<UserResponse> execute(SearchUsersQuery query, ScrollCursor cursor) {
        UserScrollFilter filter = new UserScrollFilter(query.query().orElse(null));

        CursorPageResponse<User> pageResult =
                userRepository.searchUsers(cursor, query.pageSize(), filter);

        var items = pageResult.items().stream().map(this::toResponse).toList();

        return new CursorPageResponse<>(items, pageResult.pageSize(), pageResult.hasNext());
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail().value(),
                user.getFullName().value(),
                user.getUsername().map(Username::value).orElse(null),
                user.getAvatarUrl().orElse(null),
                user.getAuthProvider(),
                preferencesParser.parseAsResponse(user.getPreferences()),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
