package io.github.phunguy65.zms.meetingmanagement.presentation;

import io.github.phunguy65.zms.meetingmanagement.application.command.CancelMeetingCommand;
import io.github.phunguy65.zms.meetingmanagement.application.command.EndMeetingCommand;
import io.github.phunguy65.zms.meetingmanagement.application.command.StartMeetingCommand;
import io.github.phunguy65.zms.meetingmanagement.application.query.GetMeetingByShortCodeQuery;
import io.github.phunguy65.zms.meetingmanagement.application.query.GetMeetingQuery;
import io.github.phunguy65.zms.meetingmanagement.application.query.ListHostMeetingsQuery;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.*;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.presentation.request.CreateInstantMeetingRequest;
import io.github.phunguy65.zms.meetingmanagement.presentation.request.ScheduleMeetingRequest;
import io.github.phunguy65.zms.meetingmanagement.presentation.request.UpdateMeetingSettingsRequest;
import io.github.phunguy65.zms.shared.domain.CursorErrorCode;
import io.github.phunguy65.zms.shared.domain.CursorTokenEncoder;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.ScrollCursor;
import io.github.phunguy65.zms.shared.infrastructure.web.CursorScrollResponse;
import io.github.phunguy65.zms.shared.infrastructure.web.FailData;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class MeetingController extends BaseController {

    private final ScheduleMeetingUseCase scheduleMeetingUseCase;
    private final CreateInstantMeetingUseCase createInstantMeetingUseCase;
    private final GetMeetingUseCase getMeetingUseCase;
    private final GetMeetingByShortCodeUseCase getMeetingByShortCodeUseCase;
    private final ListHostMeetingsUseCase listHostMeetingsUseCase;
    private final StartMeetingUseCase startMeetingUseCase;
    private final EndMeetingUseCase endMeetingUseCase;
    private final CancelMeetingUseCase cancelMeetingUseCase;
    private final UpdateMeetingSettingsUseCase updateMeetingSettingsUseCase;
    private final CursorTokenEncoder cursorTokenEncoder;

    public MeetingController(
            ScheduleMeetingUseCase scheduleMeetingUseCase,
            CreateInstantMeetingUseCase createInstantMeetingUseCase,
            GetMeetingUseCase getMeetingUseCase,
            GetMeetingByShortCodeUseCase getMeetingByShortCodeUseCase,
            ListHostMeetingsUseCase listHostMeetingsUseCase,
            StartMeetingUseCase startMeetingUseCase,
            EndMeetingUseCase endMeetingUseCase,
            CancelMeetingUseCase cancelMeetingUseCase,
            UpdateMeetingSettingsUseCase updateMeetingSettingsUseCase,
            CursorTokenEncoder cursorTokenEncoder) {
        this.scheduleMeetingUseCase = scheduleMeetingUseCase;
        this.createInstantMeetingUseCase = createInstantMeetingUseCase;
        this.getMeetingUseCase = getMeetingUseCase;
        this.getMeetingByShortCodeUseCase = getMeetingByShortCodeUseCase;
        this.listHostMeetingsUseCase = listHostMeetingsUseCase;
        this.startMeetingUseCase = startMeetingUseCase;
        this.endMeetingUseCase = endMeetingUseCase;
        this.cancelMeetingUseCase = cancelMeetingUseCase;
        this.updateMeetingSettingsUseCase = updateMeetingSettingsUseCase;
        this.cursorTokenEncoder = cursorTokenEncoder;
    }

    @PostMapping(value = "/{version}/meetings:schedule", version = "1.0")
    public ResponseEntity<JsendResponse<?>> scheduleMeeting(
            @Valid @RequestBody ScheduleMeetingRequest request, Authentication auth) {
        UUID hostId = extractUserId(auth);
        if (hostId == null) return unauthenticated();
        return switch (scheduleMeetingUseCase.execute(request.toCommand(hostId))) {
            case Result.Success<?, MeetingError> s ->
                ResponseEntity.status(HttpStatus.CREATED).body(JsendResponse.success(s.value()));
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    @PostMapping(value = "/{version}/meetings:instant", version = "1.0")
    public ResponseEntity<JsendResponse<?>> createInstantMeeting(
            @Valid @RequestBody CreateInstantMeetingRequest request, Authentication auth) {
        UUID hostId = extractUserId(auth);
        if (hostId == null) return unauthenticated();
        return switch (createInstantMeetingUseCase.execute(request.toCommand(hostId))) {
            case Result.Success<?, MeetingError> s ->
                ResponseEntity.status(HttpStatus.CREATED).body(JsendResponse.success(s.value()));
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    @GetMapping(value = "/{version}/meetings/{id}", version = "1.0")
    public ResponseEntity<JsendResponse<?>> getMeeting(@PathVariable UUID id) {
        return switch (getMeetingUseCase.execute(new GetMeetingQuery(id))) {
            case Result.Success<?, MeetingError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    @GetMapping(value = "/{version}/meetings:byShortCode", version = "1.0")
    public ResponseEntity<JsendResponse<?>> getMeetingByShortCode(@RequestParam String code) {
        return switch (getMeetingByShortCodeUseCase.execute(new GetMeetingByShortCodeQuery(code))) {
            case Result.Success<?, MeetingError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    @GetMapping(value = "/{version}/meetings", version = "1.0")
    public ResponseEntity<JsendResponse<?>> listHostMeetings(
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) @Nullable String pageToken,
            Authentication auth) {
        UUID hostId = extractUserId(auth);
        if (hostId == null) return unauthenticated();

        ListHostMeetingsQuery query = new ListHostMeetingsQuery(hostId, pageSize, pageToken);

        if (query.pageToken().isEmpty()) {
            return executeListMeetings(query, null);
        }
        var decodeResult = cursorTokenEncoder.decode(query.pageToken().get());
        return switch (decodeResult) {
            case Result.Failure<ScrollCursor, CursorErrorCode> f ->
                ResponseEntity.badRequest()
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
            case Result.Success<ScrollCursor, CursorErrorCode> s ->
                executeListMeetings(query, s.value());
        };
    }

    private ResponseEntity<JsendResponse<?>> executeListMeetings(
            ListHostMeetingsQuery query, @Nullable ScrollCursor cursor) {
        var pageResult = listHostMeetingsUseCase.execute(query, cursor);
        String nextPageToken = null;
        if (pageResult.hasNext() && !pageResult.items().isEmpty()) {
            var last = pageResult.items().getLast();
            nextPageToken = cursorTokenEncoder.encode(last.createdAt(), last.id());
        }
        var response =
                new CursorScrollResponse<>(pageResult.items(), query.pageSize(), nextPageToken);
        return ResponseEntity.ok(JsendResponse.success(response));
    }

    @PostMapping(value = "/{version}/meetings/{id}:start", version = "1.0")
    public ResponseEntity<JsendResponse<?>> startMeeting(
            @PathVariable UUID id, Authentication auth) {
        UUID requesterId = extractUserId(auth);
        if (requesterId == null) return unauthenticated();
        return switch (startMeetingUseCase.execute(new StartMeetingCommand(id, requesterId))) {
            case Result.Success<?, MeetingError> s -> ResponseEntity.ok(JsendResponse.success());
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    @PostMapping(value = "/{version}/meetings/{id}:end", version = "1.0")
    public ResponseEntity<JsendResponse<?>> endMeeting(@PathVariable UUID id, Authentication auth) {
        UUID requesterId = extractUserId(auth);
        if (requesterId == null) return unauthenticated();
        return switch (endMeetingUseCase.execute(new EndMeetingCommand(id, requesterId))) {
            case Result.Success<?, MeetingError> s -> ResponseEntity.ok(JsendResponse.success());
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    @PostMapping(value = "/{version}/meetings/{id}:cancel", version = "1.0")
    public ResponseEntity<JsendResponse<?>> cancelMeeting(
            @PathVariable UUID id, Authentication auth) {
        UUID requesterId = extractUserId(auth);
        if (requesterId == null) return unauthenticated();
        return switch (cancelMeetingUseCase.execute(new CancelMeetingCommand(id, requesterId))) {
            case Result.Success<?, MeetingError> s -> ResponseEntity.ok(JsendResponse.success());
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    @PatchMapping(value = "/{version}/meetings/{id}/settings", version = "1.0")
    public ResponseEntity<JsendResponse<?>> updateMeetingSettings(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMeetingSettingsRequest request,
            Authentication auth) {
        UUID requesterId = extractUserId(auth);
        if (requesterId == null) return unauthenticated();
        return switch (updateMeetingSettingsUseCase.execute(request.toCommand(id, requesterId))) {
            case Result.Success<?, MeetingError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }
}
