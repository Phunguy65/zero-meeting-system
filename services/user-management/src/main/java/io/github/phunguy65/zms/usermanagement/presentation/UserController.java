package io.github.phunguy65.zms.usermanagement.presentation;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.infrastructure.web.FailData;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import io.github.phunguy65.zms.usermanagement.application.dto.GetUsersRequest;
import io.github.phunguy65.zms.usermanagement.application.usecase.GetUserUseCase;
import io.github.phunguy65.zms.usermanagement.application.usecase.GetUsersSliceUseCase;
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
    private final GetUsersSliceUseCase getUsersSliceUseCase;

    public UserController(
            GetUserUseCase getUserUseCase, GetUsersSliceUseCase getUsersSliceUseCase) {
        this.getUserUseCase = getUserUseCase;
        this.getUsersSliceUseCase = getUsersSliceUseCase;
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

    @GetMapping(value = "/{version}/users", version = "1.0")
    public ResponseEntity<JsendResponse<?>> getUsers(@Valid GetUsersRequest request) {
        var result = getUsersSliceUseCase.execute(request);
        return switch (result) {
            case Result.Success<?, AuthErrorCode> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, AuthErrorCode> f ->
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
        };
    }
}
