package io.github.phunguy65.zms.usermanagement.presentation;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.infrastructure.web.FailData;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.zms.usermanagement.application.dto.GoogleLoginRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.LoginRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.LogoutRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.RefreshTokenRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.RegisterRequest;
import io.github.phunguy65.zms.usermanagement.application.usecase.DeleteAccountUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.LoginUserUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.LoginWithGoogleUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.LogoutUserUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.RefreshTokenUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.RegisterUserUseCase;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUserUseCase logoutUserUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;
    private final LoginWithGoogleUseCase loginWithGoogleUseCase;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUserUseCase loginUserUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUserUseCase logoutUserUseCase,
            DeleteAccountUseCase deleteAccountUseCase,
            LoginWithGoogleUseCase loginWithGoogleUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUserUseCase = loginUserUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUserUseCase = logoutUserUseCase;
        this.deleteAccountUseCase = deleteAccountUseCase;
        this.loginWithGoogleUseCase = loginWithGoogleUseCase;
    }

    /** POST /api/v1/auth/register */
    @PostMapping(value = "/{version}/auth/register", version = "1.0")
    public ResponseEntity<JsendResponse<?>> register(@Valid @RequestBody RegisterRequest request) {
        var result = registerUserUseCase.execute(request);
        return switch (result) {
            case Result.Success<?, AuthErrorCode> s ->
                ResponseEntity.status(HttpStatus.CREATED).body(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthErrorCode> f -> {
                if (f.error() == AuthErrorCode.EMAIL_ALREADY_EXISTS) {
                    yield ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(JsendResponse.fail(
                                    new FailData(f.error().name(), f.error(), List.of())));
                }
                yield ResponseEntity.badRequest()
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
            }
        };
    }

    /** POST /api/v1/auth/login */
    @PostMapping(value = "/{version}/auth/login", version = "1.0")
    public ResponseEntity<JsendResponse<?>> login(@Valid @RequestBody LoginRequest request) {
        var result = loginUserUseCase.execute(request);
        return switch (result) {
            case Result.Success<?, AuthErrorCode> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthErrorCode> f ->
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
        };
    }

    /** POST /api/v1/auth/refresh */
    @PostMapping(value = "/{version}/auth/refresh", version = "1.0")
    public ResponseEntity<JsendResponse<?>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        var result = refreshTokenUseCase.execute(request);
        return switch (result) {
            case Result.Success<?, AuthErrorCode> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthErrorCode> f ->
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
        };
    }

    /** POST /api/v1/auth/logout */
    @PostMapping(value = "/{version}/auth/logout", version = "1.0")
    public ResponseEntity<JsendResponse<?>> logout(@Valid @RequestBody LogoutRequest request) {
        var result = logoutUserUseCase.execute(request);
        return switch (result) {
            case Result.Success<?, AuthErrorCode> _ -> ResponseEntity.ok(JsendResponse.success());
            case Result.Failure<?, AuthErrorCode> f ->
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
        };
    }

    /** POST /api/v1/auth/google-login */
    @PostMapping(value = "/{version}/auth/google-login", version = "1.0")
    public ResponseEntity<JsendResponse<?>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request) {
        var result = loginWithGoogleUseCase.execute(request);
        return switch (result) {
            case Result.Success<?, AuthErrorCode> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthErrorCode> f ->
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
        };
    }

    /** DELETE /api/v1/auth/me */
    @DeleteMapping(value = "/{version}/auth/me", version = "1.0")
    public ResponseEntity<JsendResponse<?>> deleteAccount() {
        String principalId =
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(principalId);

        var result = deleteAccountUseCase.execute(userId);
        return switch (result) {
            case Result.Success<?, AuthErrorCode> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthErrorCode> f ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
        };
    }
}
