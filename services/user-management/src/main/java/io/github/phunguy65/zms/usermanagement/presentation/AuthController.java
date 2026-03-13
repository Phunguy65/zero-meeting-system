package io.github.phunguy65.zms.usermanagement.presentation;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.zms.usermanagement.application.usecase.LoginUserUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.LoginWithGoogleUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.LogoutUserUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.RefreshTokenUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.RegisterUserUseCase;
import io.github.phunguy65.zms.usermanagement.domain.AuthError;
import io.github.phunguy65.zms.usermanagement.presentation.request.GoogleLoginRequest;
import io.github.phunguy65.zms.usermanagement.presentation.request.LoginRequest;
import io.github.phunguy65.zms.usermanagement.presentation.request.LogoutRequest;
import io.github.phunguy65.zms.usermanagement.presentation.request.RefreshTokenRequest;
import io.github.phunguy65.zms.usermanagement.presentation.request.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController extends BaseController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUserUseCase logoutUserUseCase;
    private final LoginWithGoogleUseCase loginWithGoogleUseCase;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUserUseCase loginUserUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUserUseCase logoutUserUseCase,
            LoginWithGoogleUseCase loginWithGoogleUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUserUseCase = loginUserUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUserUseCase = logoutUserUseCase;
        this.loginWithGoogleUseCase = loginWithGoogleUseCase;
    }

    @PostMapping(value = "/{version}/auth/register", version = "1.0")
    public ResponseEntity<JsendResponse<?>> register(@Valid @RequestBody RegisterRequest request) {
        return switch (registerUserUseCase.execute(request.toCommand())) {
            case Result.Success<?, AuthError> s ->
                ResponseEntity.status(HttpStatus.CREATED).body(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthError> f -> errorResponse(f.error());
        };
    }

    @PostMapping(value = "/{version}/auth/login", version = "1.0")
    public ResponseEntity<JsendResponse<?>> login(@Valid @RequestBody LoginRequest request) {
        return switch (loginUserUseCase.execute(request.toCommand())) {
            case Result.Success<?, AuthError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthError> f -> errorResponse(f.error());
        };
    }

    @PostMapping(value = "/{version}/auth/refresh", version = "1.0")
    public ResponseEntity<JsendResponse<?>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return switch (refreshTokenUseCase.execute(request.toCommand())) {
            case Result.Success<?, AuthError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthError> f -> errorResponse(f.error());
        };
    }

    @PostMapping(value = "/{version}/auth/logout", version = "1.0")
    public ResponseEntity<JsendResponse<?>> logout(@Valid @RequestBody LogoutRequest request) {
        return switch (logoutUserUseCase.execute(request.toCommand())) {
            case Result.Success<?, AuthError> _ -> ResponseEntity.ok(JsendResponse.success());
            case Result.Failure<?, AuthError> f -> errorResponse(f.error());
        };
    }

    @PostMapping(value = "/{version}/auth/google-login", version = "1.0")
    public ResponseEntity<JsendResponse<?>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request) {
        return switch (loginWithGoogleUseCase.execute(request.toCommand())) {
            case Result.Success<?, AuthError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthError> f -> errorResponse(f.error());
        };
    }
}
