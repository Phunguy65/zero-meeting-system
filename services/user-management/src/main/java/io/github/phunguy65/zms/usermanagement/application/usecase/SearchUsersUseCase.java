package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.CursorPageResult;
import io.github.phunguy65.zms.shared.domain.ScrollCursor;
import io.github.phunguy65.zms.usermanagement.application.dto.SearchUsersRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.UserResponse;
import io.github.phunguy65.zms.usermanagement.application.service.UserResponseMapper;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import io.github.phunguy65.zms.usermanagement.domain.port.UserScrollFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchUsersUseCase {

    private final UserRepository userRepository;
    private final UserResponseMapper userResponseMapper;

    public SearchUsersUseCase(
            UserRepository userRepository, UserResponseMapper userResponseMapper) {
        this.userRepository = userRepository;
        this.userResponseMapper = userResponseMapper;
    }

    /**
     * Searches users by optional query string with keyset pagination.
     *
     * <p>Cursor decoding is delegated to the caller (controller) via {@link CursorTokenEncoder}.
     * The returned {@link CursorPageResult} carries the raw domain items and a flag indicating
     * whether more pages exist; the controller is responsible for encoding the next page token and
     * wrapping the result in the HTTP response envelope.
     *
     * @param request pagination and filter parameters
     * @param cursor  decoded cursor from the previous page, or {@code null} for the first page
     * @return a page of {@link UserResponse} items
     */
    @Transactional(readOnly = true)
    public CursorPageResult<UserResponse> execute(SearchUsersRequest request, ScrollCursor cursor) {
        UserScrollFilter filter = new UserScrollFilter(request.query().orElse(null));

        CursorPageResult<User> pageResult =
                userRepository.searchUsers(cursor, request.pageSize(), filter);

        var items =
                pageResult.items().stream().map(userResponseMapper::toResponse).toList();

        return new CursorPageResult<>(items, pageResult.pageSize(), pageResult.hasNext());
    }
}
