package io.github.phunguy65.zms.usermanagement.presentation;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.infrastructure.web.FailData;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.zms.usermanagement.application.dto.PatchPreferencesRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.PatchUserRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.UserPreferencesRequest;
import io.github.phunguy65.zms.usermanagement.application.usecase.GetUserPreferencesUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.GetUserUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.GetUsersSliceUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.PatchUpdatePreferencesUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.PatchUpdateUserUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.UpdateUserPreferencesUseCase;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.port.UserFilter;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    private final GetUserPreferencesUseCase getPreferencesUseCase;
    private final UpdateUserPreferencesUseCase updatePreferencesUseCase;
    private final GetUserUseCase getUserUseCase;
    private final GetUsersSliceUseCase getUsersSliceUseCase;
    private final PatchUpdateUserUseCase patchUpdateUserUseCase;
    private final PatchUpdatePreferencesUseCase patchUpdatePreferencesUseCase;

    public UserController(
            GetUserPreferencesUseCase getPreferencesUseCase,
            UpdateUserPreferencesUseCase updatePreferencesUseCase,
            GetUserUseCase getUserUseCase,
            GetUsersSliceUseCase getUsersSliceUseCase,
            PatchUpdateUserUseCase patchUpdateUserUseCase,
            PatchUpdatePreferencesUseCase patchUpdatePreferencesUseCase) {
        this.getPreferencesUseCase = getPreferencesUseCase;
        this.updatePreferencesUseCase = updatePreferencesUseCase;
        this.getUserUseCase = getUserUseCase;
        this.getUsersSliceUseCase = getUsersSliceUseCase;
        this.patchUpdateUserUseCase = patchUpdateUserUseCase;
        this.patchUpdatePreferencesUseCase = patchUpdatePreferencesUseCase;
    }

    /** GET /api/v1/users/me/preferences */
    @GetMapping(value = "/{version}/users/me/preferences", version = "1.0")
    public ResponseEntity<JsendResponse<?>> getPreferences() {
        UUID userId = currentUserId();
        var result = getPreferencesUseCase.execute(userId);
        return switch (result) {
            case Result.Success<?, AuthErrorCode> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthErrorCode> f ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
        };
    }

    /** PUT /api/v1/users/me/preferences */
    @PutMapping(value = "/{version}/users/me/preferences", version = "1.0")
    public ResponseEntity<JsendResponse<?>> updatePreferences(
            @RequestBody Map<String, Object> body) {
        UUID userId = currentUserId();
        var result = updatePreferencesUseCase.execute(userId, new UserPreferencesRequest(body));
        return switch (result) {
            case Result.Success<?, AuthErrorCode> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthErrorCode> f ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
        };
    }

    /** GET /api/v1/users/me */
    @GetMapping(value = "/{version}/users/me", version = "1.0")
    public ResponseEntity<JsendResponse<?>> getMe() {
        var result = getUserUseCase.execute(currentUserId());
        return toUserResponse(result);
    }

    /** GET /api/v1/users/{id} */
    @GetMapping(value = "/{version}/users/{id}", version = "1.0")
    public ResponseEntity<JsendResponse<?>> getUserById(@PathVariable UUID id) {
        var result = getUserUseCase.execute(id);
        return toUserResponse(result);
    }

    /** GET /api/v1/users */
    @GetMapping(value = "/{version}/users", version = "1.0")
    public ResponseEntity<JsendResponse<?>> getUsers(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String authProvider,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int clampedSize = Math.min(size, 100);
        var filter = new UserFilter(email, authProvider);
        var result = getUsersSliceUseCase.execute(page, clampedSize, filter);
        return switch (result) {
            case Result.Success<?, AuthErrorCode> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthErrorCode> f ->
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
        };
    }

    /** PATCH /api/v1/users/me */
    @PatchMapping(value = "/{version}/users/me", version = "1.0")
    public ResponseEntity<JsendResponse<?>> patchMe(@Valid @RequestBody PatchUserRequest dto) {
        var result = patchUpdateUserUseCase.execute(currentUserId(), dto);
        return toUserResponse(result);
    }

    /** PATCH /api/v1/users/me/preferences */
    @PatchMapping(value = "/{version}/users/me/preferences", version = "1.0")
    public ResponseEntity<JsendResponse<?>> patchPreferences(
            @RequestBody Map<String, Object> body) {
        var dto = new PatchPreferencesRequest(JsonNullable.of(body));
        var result = patchUpdatePreferencesUseCase.execute(currentUserId(), dto);
        return switch (result) {
            case Result.Success<?, AuthErrorCode> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthErrorCode> f ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
        };
    }

    private ResponseEntity<JsendResponse<?>> toUserResponse(Result<?, AuthErrorCode> result) {
        return switch (result) {
            case Result.Success<?, AuthErrorCode> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthErrorCode> f ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
        };
    }

    private UUID currentUserId() {
        String principalId =
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(principalId);
    }
}
