package io.github.phunguy65.zms.meetingmanagement.presentation;

import io.github.phunguy65.zms.meetingmanagement.application.command.ApproveAllJoinRequestsCommand;
import io.github.phunguy65.zms.meetingmanagement.application.command.ApproveJoinRequestCommand;
import io.github.phunguy65.zms.meetingmanagement.application.command.DenyJoinRequestCommand;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.*;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.infrastructure.sse.MeetingSseManager;
import io.github.phunguy65.zms.meetingmanagement.presentation.request.JoinRequestRequest;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class JoinRequestController extends BaseController {

    private final RequestJoinUseCase requestJoinUseCase;
    private final ApproveJoinRequestUseCase approveJoinRequestUseCase;
    private final DenyJoinRequestUseCase denyJoinRequestUseCase;
    private final ApproveAllJoinRequestsUseCase approveAllJoinRequestsUseCase;
    private final ListJoinRequestsUseCase listJoinRequestsUseCase;
    private final MeetingSseManager meetingSseManager;

    public JoinRequestController(
            RequestJoinUseCase requestJoinUseCase,
            ApproveJoinRequestUseCase approveJoinRequestUseCase,
            DenyJoinRequestUseCase denyJoinRequestUseCase,
            ApproveAllJoinRequestsUseCase approveAllJoinRequestsUseCase,
            ListJoinRequestsUseCase listJoinRequestsUseCase,
            MeetingSseManager meetingSseManager) {
        this.requestJoinUseCase = requestJoinUseCase;
        this.approveJoinRequestUseCase = approveJoinRequestUseCase;
        this.denyJoinRequestUseCase = denyJoinRequestUseCase;
        this.approveAllJoinRequestsUseCase = approveAllJoinRequestsUseCase;
        this.listJoinRequestsUseCase = listJoinRequestsUseCase;
        this.meetingSseManager = meetingSseManager;
    }

    @PostMapping(value = "/{version}/meetings/{id}:requestJoin", version = "1.0")
    public ResponseEntity<JsendResponse<?>> requestJoin(
            @PathVariable UUID id,
            @Valid @RequestBody JoinRequestRequest request,
            Authentication auth) {
        UUID userId = extractUserId(auth);
        return switch (requestJoinUseCase.execute(request.toCommand(id, userId))) {
            case Result.Success<?, MeetingError> s -> {
                var body = s.value();
                var response = (io.github.phunguy65.zms.meetingmanagement.application.response
                                .RequestJoinResponse)
                        body;
                int httpStatus = response.status()
                                == io.github.phunguy65.zms.meetingmanagement.domain.model
                                        .JoinRequestStatus.PENDING
                        ? 202
                        : 200;
                yield ResponseEntity.status(httpStatus).body(JsendResponse.success(body));
            }
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    @GetMapping(value = "/{version}/meetings/{id}/joinRequests", version = "1.0")
    public ResponseEntity<JsendResponse<?>> listJoinRequests(
            @PathVariable UUID id, Authentication auth) {
        UUID requesterId = extractUserId(auth);
        if (requesterId == null) return unauthenticated();
        return switch (listJoinRequestsUseCase.execute(id, requesterId)) {
            case Result.Success<?, MeetingError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    @PostMapping(
            value = "/{version}/meetings/{id}/joinRequests/{requestId}:approve",
            version = "1.0")
    public ResponseEntity<JsendResponse<?>> approveJoinRequest(
            @PathVariable UUID id, @PathVariable UUID requestId, Authentication auth) {
        UUID approvedBy = extractUserId(auth);
        if (approvedBy == null) return unauthenticated();
        return switch (approveJoinRequestUseCase.execute(
                new ApproveJoinRequestCommand(id, requestId, approvedBy))) {
            case Result.Success<?, MeetingError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    @PostMapping(value = "/{version}/meetings/{id}/joinRequests/{requestId}:deny", version = "1.0")
    public ResponseEntity<JsendResponse<?>> denyJoinRequest(
            @PathVariable UUID id, @PathVariable UUID requestId, Authentication auth) {
        UUID deniedBy = extractUserId(auth);
        if (deniedBy == null) return unauthenticated();
        return switch (denyJoinRequestUseCase.execute(
                new DenyJoinRequestCommand(id, requestId, deniedBy))) {
            case Result.Success<?, MeetingError> s ->
                ResponseEntity.ok(JsendResponse.success(null));
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    @PostMapping(value = "/{version}/meetings/{id}/joinRequests:approveAll", version = "1.0")
    public ResponseEntity<JsendResponse<?>> approveAllJoinRequests(
            @PathVariable UUID id, Authentication auth) {
        UUID approvedBy = extractUserId(auth);
        if (approvedBy == null) return unauthenticated();
        return switch (approveAllJoinRequestsUseCase.execute(
                new ApproveAllJoinRequestsCommand(id, approvedBy))) {
            case Result.Success<?, MeetingError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    /**
     * GET /v1.0/joinRequests/{requestId}/events — SSE stream for guest awaiting join request
     * resolution (no auth required).
     *
     * <p>The stream receives {@code join_request_approved}, {@code join_request_denied}, or
     * {@code join_request_expired} events and closes automatically once the request is resolved.
     */
    @GetMapping(
            value = "/{version}/joinRequests/{requestId}/events",
            version = "1.0",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> subscribeGuestToEvents(@PathVariable UUID requestId) {
        SseEmitter emitter = meetingSseManager.subscribeGuest(requestId);
        return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(emitter);
    }

    /** GET /v1.0/meetings/{id}/events — SSE stream for host (requires auth) */
    @GetMapping(
            value = "/{version}/meetings/{id}/events",
            version = "1.0",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> subscribeToEvents(@PathVariable UUID id, Authentication auth) {
        UUID userId = extractUserId(auth);
        if (userId == null) return unauthenticated();
        SseEmitter emitter = meetingSseManager.subscribeHost(id, userId);
        return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(emitter);
    }
}
