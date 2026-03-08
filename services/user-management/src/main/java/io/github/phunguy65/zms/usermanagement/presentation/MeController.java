package io.github.phunguy65.zms.usermanagement.presentation;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.infrastructure.web.FailData;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.zms.usermanagement.application.dto.PatchPreferencesRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.PatchUserRequest;
import io.github.phunguy65.zms.usermanagement.application.usecase.DeleteAccountUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.GetUserPreferencesUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.GetUserUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.PatchUpdatePreferencesUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.PatchUpdateUserUseCase;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class MeController {

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
            return unauthorizedResponse();
        }
        return switch (getUserUseCase.execute(userId)) {
            case Result.Success<?, AuthErrorCode> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthErrorCode> f ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
        };
    }

    @PatchMapping(value = "/{version}/me", version = "1.0")
    public ResponseEntity<JsendResponse<?>> patchMe(
            @Valid @RequestBody PatchUserRequest dto, Authentication auth) {
        UUID userId = extractUserId(auth);
        if (userId == null) {
            return unauthorizedResponse();
        }
        return switch (patchUpdateUserUseCase.execute(userId, dto)) {
            case Result.Success<?, AuthErrorCode> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthErrorCode> f -> {
                yield switch (f.error()) {
                    case USERNAME_ALREADY_EXISTS ->
                        ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(JsendResponse.fail(
                                        new FailData(f.error().name(), f.error(), List.of())));
                    case PREFERENCES_SERIALIZATION_ERROR ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(JsendResponse.error(f.error().name()));
                    default ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(JsendResponse.fail(
                                        new FailData(f.error().name(), f.error(), List.of())));
                };
            }
        };
    }

    @GetMapping(value = "/{version}/me/preferences", version = "1.0")
    public ResponseEntity<JsendResponse<?>> getPreferences(Authentication auth) {
        UUID userId = extractUserId(auth);
        if (userId == null) {
            return unauthorizedResponse();
        }
        return switch (getPreferencesUseCase.execute(userId)) {
            case Result.Success<?, AuthErrorCode> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthErrorCode> f ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
        };
    }

    @PatchMapping(value = "/{version}/me/preferences", version = "1.0")
    public ResponseEntity<JsendResponse<?>> patchPreferences(
            @Valid @RequestBody Map<String, Object> body, Authentication auth) {
        UUID userId = extractUserId(auth);
        if (userId == null) {
            return unauthorizedResponse();
        }
        var dto = new PatchPreferencesRequest(JsonNullable.of(body));
        return switch (patchUpdatePreferencesUseCase.execute(userId, dto)) {
            case Result.Success<?, AuthErrorCode> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthErrorCode> f -> {
                yield switch (f.error()) {
                    case PREFERENCES_SERIALIZATION_ERROR ->
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(JsendResponse.error(f.error().name()));
                    default ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(JsendResponse.fail(
                                        new FailData(f.error().name(), f.error(), List.of())));
                };
            }
        };
    }

    @DeleteMapping(value = "/{version}/me", version = "1.0")
    public ResponseEntity<JsendResponse<?>> deleteMe(Authentication auth) {
        UUID userId = extractUserId(auth);
        if (userId == null) {
            return unauthorizedResponse();
        }
        return switch (deleteAccountUseCase.execute(userId)) {
            case Result.Success<?, AuthErrorCode> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthErrorCode> f ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
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

    private ResponseEntity<JsendResponse<?>> unauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(JsendResponse.fail(new FailData(
                        AuthErrorCode.INVALID_CREDENTIALS.name(),
                        AuthErrorCode.INVALID_CREDENTIALS,
                        List.of())));
    }
}
