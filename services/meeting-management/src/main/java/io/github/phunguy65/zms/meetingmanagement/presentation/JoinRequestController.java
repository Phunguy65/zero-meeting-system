package io.github.phunguy65.zms.meetingmanagement.presentation;

import io.github.phunguy65.zms.meetingmanagement.application.command.ApproveAllJoinRequestsCommand;
import io.github.phunguy65.zms.meetingmanagement.application.command.ApproveJoinRequestCommand;
import io.github.phunguy65.zms.meetingmanagement.application.command.DenyJoinRequestCommand;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.ApproveAllJoinRequestsUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.ApproveJoinRequestUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.DenyJoinRequestUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.ListJoinRequestsUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.PollJoinRequestStatusUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.RequestJoinUseCase;
import io.github.phunguy65.zms.meetingmanagement.infrastructure.sse.MeetingSseManager;
import io.github.phunguy65.zms.meetingmanagement.presentation.request.JoinRequestRequest;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
public class JoinRequestController extends BaseController {

    private final RequestJoinUseCase requestJoinUseCase;
    private final ApproveJoinRequestUseCase approveJoinRequestUseCase;
    private final DenyJoinRequestUseCase denyJoinRequestUseCase;
    private final ApproveAllJoinRequestsUseCase approveAllJoinRequestsUseCase;
    private final ListJoinRequestsUseCase listJoinRequestsUseCase;
    private final PollJoinRequestStatusUseCase pollJoinRequestStatusUseCase;
    private final MeetingSseManager meetingSseManager;

    public JoinRequestController(
            RequestJoinUseCase requestJoinUseCase,
            ApproveJoinRequestUseCase approveJoinRequestUseCase,
            DenyJoinRequestUseCase denyJoinRequestUseCase,
            ApproveAllJoinRequestsUseCase approveAllJoinRequestsUseCase,
            ListJoinRequestsUseCase listJoinRequestsUseCase,
            PollJoinRequestStatusUseCase pollJoinRequestStatusUseCase,
            MeetingSseManager meetingSseManager) {
        this.requestJoinUseCase = requestJoinUseCase;
        this.approveJoinRequestUseCase = approveJoinRequestUseCase;
        this.denyJoinRequestUseCase = denyJoinRequestUseCase;
        this.approveAllJoinRequestsUseCase = approveAllJoinRequestsUseCase;
        this.listJoinRequestsUseCase = listJoinRequestsUseCase;
        this.pollJoinRequestStatusUseCase = pollJoinRequestStatusUseCase;
        this.meetingSseManager = meetingSseManager;
    }

    /** POST /v1.0/meetings/{id}:requestJoin — submit a join request (no auth required for guests) */
    @PostMapping(value = "/{version}/meetings/{id}:requestJoin", version = "1.0")
    public ResponseEntity<JsendResponse<?>> requestJoin(
            @PathVariable UUID id,
            @Valid @RequestBody JoinRequestRequest request,
            Authentication auth) {
        UUID userId = extractUserId(auth); // null for guests
        return switch (requestJoinUseCase.execute(request.toCommand(id, userId))) {
            case Result.Success<?, MeetingError> s -> {
                var body = s.value();
                // Return 202 for PENDING, 200 for APPROVED
                var response = (io.github.phunguy65.zms.meetingmanagement.application.response.RequestJoinResponse) body;
                int httpStatus = response.status() ==
                        io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequestStatus.PENDING
                        ? 202 : 200;
                yield ResponseEntity.status(httpStatus).body(JsendResponse.success(body));
            }
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    /** GET /v1.0/meetings/{id}/joinRequests — list pending requests (host only) */
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

    /** POST /v1.0/meetings/{id}/joinRequests/{requestId}:approve — approve a single request */
    @PostMapping(value = "/{version}/meetings/{id}/joinRequests/{requestId}:approve", version = "1.0")
    public ResponseEntity<JsendResponse<?>> approveJoinRequest(
            @PathVariable UUID id,
            @PathVariable UUID requestId,
            Authentication auth) {
        UUID approvedBy = extractUserId(auth);
        if (approvedBy == null) return unauthenticated();
        return switch (approveJoinRequestUseCase.execute(
                new ApproveJoinRequestCommand(id, requestId, approvedBy))) {
            case Result.Success<?, MeetingError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    /** POST /v1.0/meetings/{id}/joinRequests/{requestId}:deny — deny a single request */
    @PostMapping(value = "/{version}/meetings/{id}/joinRequests/{requestId}:deny", version = "1.0")
    public ResponseEntity<JsendResponse<?>> denyJoinRequest(
            @PathVariable UUID id,
            @PathVariable UUID requestId,
            Authentication auth) {
        UUID deniedBy = extractUserId(auth);
        if (deniedBy == null) return unauthenticated();
        return switch (denyJoinRequestUseCase.execute(
                new DenyJoinRequestCommand(id, requestId, deniedBy))) {
            case Result.Success<?, MeetingError> s ->
                ResponseEntity.ok(JsendResponse.success(null));
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    /** POST /v1.0/meetings/{id}/joinRequests:approveAll — approve all pending requests */
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

    /** GET /v1.0/joinRequests/{requestId} — poll status (no auth required) */
    @GetMapping(value = "/{version}/joinRequests/{requestId}", version = "1.0")
    public ResponseEntity<JsendResponse<?>> pollJoinRequestStatus(
            @PathVariable UUID requestId) {
        return switch (pollJoinRequestStatusUseCase.execute(requestId)) {
            case Result.Success<?, MeetingError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    /** GET /v1.0/meetings/{id}/events — SSE stream for host (requires auth) */
    @GetMapping(
            value = "/{version}/meetings/{id}/events",
            version = "1.0",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<?> subscribeToEvents(
            @PathVariable UUID id, Authentication auth) {
        UUID userId = extractUserId(auth);
        if (userId == null) return unauthenticated();
        SseEmitter emitter = meetingSseManager.subscribe(id, userId);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }
}
