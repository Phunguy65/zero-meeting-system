package io.github.phunguy65.zms.usermanagement.presentation;

import io.github.phunguy65.zms.shared.domain.CursorErrorCode;
import io.github.phunguy65.zms.shared.domain.CursorTokenEncoder;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.ScrollCursor;
import io.github.phunguy65.zms.shared.infrastructure.web.CursorScrollResponse;
import io.github.phunguy65.zms.shared.infrastructure.web.FailData;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.zms.usermanagement.application.dto.SearchUsersRequest;
import io.github.phunguy65.zms.usermanagement.application.usecase.GetUserUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.SearchUsersUseCase;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    private final GetUserUseCase getUserUseCase;
    private final SearchUsersUseCase searchUsersUseCase;
    private final CursorTokenEncoder cursorTokenEncoder;

    public UserController(
            GetUserUseCase getUserUseCase,
            SearchUsersUseCase searchUsersUseCase,
            CursorTokenEncoder cursorTokenEncoder) {
        this.getUserUseCase = getUserUseCase;
        this.searchUsersUseCase = searchUsersUseCase;
        this.cursorTokenEncoder = cursorTokenEncoder;
    }

    @GetMapping(value = "/{version}/users/{id}", version = "1.0")
    public ResponseEntity<JsendResponse<?>> getUserById(@PathVariable UUID id) {
        var result = getUserUseCase.execute(id);
        return switch (result) {
            case Result.Success<?, AuthErrorCode> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthErrorCode> f ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
        };
    }

    @GetMapping(value = "/{version}/users:search", version = "1.0")
    public ResponseEntity<JsendResponse<?>> searchUsers(
            @Valid @ModelAttribute SearchUsersRequest request) {
        if (request.pageToken().isEmpty()) {
            return executeSearch(request, null);
        }
        var decodeResult = cursorTokenEncoder.decode(request.pageToken().get());
        return switch (decodeResult) {
            case Result.Failure<ScrollCursor, CursorErrorCode> f ->
                ResponseEntity.badRequest()
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
            case Result.Success<ScrollCursor, CursorErrorCode> s ->
                executeSearch(request, s.value());
        };
    }

    private ResponseEntity<JsendResponse<?>> executeSearch(
            SearchUsersRequest request, ScrollCursor cursor) {
        var pageResult = searchUsersUseCase.execute(request, cursor);

        String nextPageToken = null;
        if (pageResult.hasNext() && !pageResult.items().isEmpty()) {
            var last = pageResult.items().getLast();
            nextPageToken = cursorTokenEncoder.encode(last.createdAt(), last.id());
        }

        var response =
                new CursorScrollResponse<>(pageResult.items(), request.pageSize(), nextPageToken);
        return ResponseEntity.ok(JsendResponse.success(response));
    }
}
