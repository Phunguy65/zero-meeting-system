package io.github.phunguy65.zms.usermanagement.presentation;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.infrastructure.web.FailData;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.zms.usermanagement.application.dto.UserPreferencesRequest;
import io.github.phunguy65.zms.usermanagement.application.usecase.GetUserPreferencesUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.UpdateUserPreferencesUseCase;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    private final GetUserPreferencesUseCase getPreferencesUseCase;
    private final UpdateUserPreferencesUseCase updatePreferencesUseCase;

    public UserController(
            GetUserPreferencesUseCase getPreferencesUseCase,
            UpdateUserPreferencesUseCase updatePreferencesUseCase) {
        this.getPreferencesUseCase = getPreferencesUseCase;
        this.updatePreferencesUseCase = updatePreferencesUseCase;
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
            @Valid @RequestBody UserPreferencesRequest dto) {
        UUID userId = currentUserId();
        var result = updatePreferencesUseCase.execute(userId, dto);
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
