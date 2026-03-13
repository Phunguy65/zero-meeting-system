package io.github.phunguy65.zms.usermanagement.presentation;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.zms.usermanagement.application.usecase.DeleteAccountUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.GetUserPreferencesUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.GetUserUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.PatchUpdatePreferencesUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.PatchUpdateUserUseCase;
import io.github.phunguy65.zms.usermanagement.domain.AuthError;
import io.github.phunguy65.zms.usermanagement.presentation.request.PatchPreferencesRequest;
import io.github.phunguy65.zms.usermanagement.presentation.request.PatchUserRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class MeController extends BaseController {

    private final GetUserUseCase getUserUseCase;
    private final PatchUpdateUserUseCase patchUpdateUserUseCase;
    private final GetUserPreferencesUseCase getPreferencesUseCase;
    private final PatchUpdatePreferencesUseCase patchUpdatePreferencesUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;

    public MeController(
            GetUserUseCase getUserUseCase,
            PatchUpdateUserUseCase patchUpdateUserUseCase,
            GetUserPreferencesUseCase getPreferencesUseCase,
            PatchUpdatePreferencesUseCase patchUpdatePreferencesUseCase,
            DeleteAccountUseCase deleteAccountUseCase) {
        this.getUserUseCase = getUserUseCase;
        this.patchUpdateUserUseCase = patchUpdateUserUseCase;
        this.getPreferencesUseCase = getPreferencesUseCase;
        this.patchUpdatePreferencesUseCase = patchUpdatePreferencesUseCase;
        this.deleteAccountUseCase = deleteAccountUseCase;
    }

    @GetMapping(value = "/{version}/me", version = "1.0")
    public ResponseEntity<JsendResponse<?>> getMe(Authentication auth) {
        UUID userId = extractUserId(auth);
        if (userId == null) {
            return errorResponse(new AuthError.InvalidCredentials());
        }
        return switch (getUserUseCase.execute(userId)) {
            case Result.Success<?, AuthError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthError> f -> errorResponse(f.error());
        };
    }

    @PatchMapping(value = "/{version}/me", version = "1.0")
    public ResponseEntity<JsendResponse<?>> patchMe(
            @Valid @RequestBody PatchUserRequest dto, Authentication auth) {
        UUID userId = extractUserId(auth);
        if (userId == null) {
            return errorResponse(new AuthError.InvalidCredentials());
        }
        return switch (patchUpdateUserUseCase.execute(userId, dto.toCommand())) {
            case Result.Success<?, AuthError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthError> f -> errorResponse(f.error());
        };
    }

    @GetMapping(value = "/{version}/me/preferences", version = "1.0")
    public ResponseEntity<JsendResponse<?>> getPreferences(Authentication auth) {
        UUID userId = extractUserId(auth);
        if (userId == null) {
            return errorResponse(new AuthError.InvalidCredentials());
        }
        return switch (getPreferencesUseCase.execute(userId)) {
            case Result.Success<?, AuthError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthError> f -> errorResponse(f.error());
        };
    }

    @PatchMapping(value = "/{version}/me/preferences", version = "1.0")
    public ResponseEntity<JsendResponse<?>> patchPreferences(
            @Valid @RequestBody Map<String, Object> body, Authentication auth) {
        UUID userId = extractUserId(auth);
        if (userId == null) {
            return errorResponse(new AuthError.InvalidCredentials());
        }
        var dto = new PatchPreferencesRequest(JsonNullable.of(body));
        return switch (patchUpdatePreferencesUseCase.execute(userId, dto.toCommand())) {
            case Result.Success<?, AuthError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthError> f -> errorResponse(f.error());
        };
    }

    @DeleteMapping(value = "/{version}/me", version = "1.0")
    public ResponseEntity<JsendResponse<?>> deleteMe(Authentication auth) {
        UUID userId = extractUserId(auth);
        if (userId == null) {
            return errorResponse(new AuthError.InvalidCredentials());
        }
        return switch (deleteAccountUseCase.execute(userId)) {
            case Result.Success<?, AuthError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthError> f -> errorResponse(f.error());
        };
    }

    private UUID extractUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof String principalId)) {
            return null;
        }
        try {
            return UUID.fromString(principalId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
