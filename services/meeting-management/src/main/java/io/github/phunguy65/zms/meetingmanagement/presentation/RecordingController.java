package io.github.phunguy65.zms.meetingmanagement.presentation;

import io.github.phunguy65.zms.meetingmanagement.application.command.StartRecordingCommand;
import io.github.phunguy65.zms.meetingmanagement.application.command.StopRecordingCommand;
import io.github.phunguy65.zms.meetingmanagement.application.query.GetRecordingQuery;
import io.github.phunguy65.zms.meetingmanagement.application.query.ListMeetingRecordingsQuery;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.CompleteRecordingUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.GetRecordingUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.ListMeetingRecordingsUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.StartRecordingUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.StopRecordingUseCase;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.presentation.request.CompleteRecordingRequest;
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
public class RecordingController extends BaseController {

    private final StartRecordingUseCase startRecordingUseCase;
    private final StopRecordingUseCase stopRecordingUseCase;
    private final CompleteRecordingUseCase completeRecordingUseCase;
    private final GetRecordingUseCase getRecordingUseCase;
    private final ListMeetingRecordingsUseCase listMeetingRecordingsUseCase;
    private final CursorTokenEncoder cursorTokenEncoder;

    public RecordingController(
            StartRecordingUseCase startRecordingUseCase,
            StopRecordingUseCase stopRecordingUseCase,
            CompleteRecordingUseCase completeRecordingUseCase,
            GetRecordingUseCase getRecordingUseCase,
            ListMeetingRecordingsUseCase listMeetingRecordingsUseCase,
            CursorTokenEncoder cursorTokenEncoder) {
        this.startRecordingUseCase = startRecordingUseCase;
        this.stopRecordingUseCase = stopRecordingUseCase;
        this.completeRecordingUseCase = completeRecordingUseCase;
        this.getRecordingUseCase = getRecordingUseCase;
        this.listMeetingRecordingsUseCase = listMeetingRecordingsUseCase;
        this.cursorTokenEncoder = cursorTokenEncoder;
    }

    @PostMapping(value = "/{version}/meetings/{id}/recordings:start", version = "1.0")
    public ResponseEntity<JsendResponse<?>> startRecording(
            @PathVariable UUID id, Authentication auth) {
        UUID requesterId = extractUserId(auth);
        if (requesterId == null) return unauthenticated();
        return switch (startRecordingUseCase.execute(new StartRecordingCommand(id, requesterId))) {
            case Result.Success<?, MeetingError> s ->
                ResponseEntity.status(HttpStatus.CREATED).body(JsendResponse.success(s.value()));
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    @PostMapping(value = "/{version}/meetings/{id}/recordings:stop", version = "1.0")
    public ResponseEntity<JsendResponse<?>> stopRecording(
            @PathVariable UUID id, Authentication auth) {
        UUID requesterId = extractUserId(auth);
        if (requesterId == null) return unauthenticated();
        return switch (stopRecordingUseCase.execute(new StopRecordingCommand(id, requesterId))) {
            case Result.Success<?, MeetingError> s -> ResponseEntity.ok(JsendResponse.success());
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    @PostMapping(value = "/{version}/recordings/{id}:complete", version = "1.0")
    public ResponseEntity<JsendResponse<?>> completeRecording(
            @PathVariable UUID id, @Valid @RequestBody CompleteRecordingRequest request) {
        return switch (completeRecordingUseCase.execute(request.toCommand(id))) {
            case Result.Success<?, MeetingError> s -> ResponseEntity.ok(JsendResponse.success());
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    @GetMapping(value = "/{version}/recordings/{id}", version = "1.0")
    public ResponseEntity<JsendResponse<?>> getRecording(@PathVariable UUID id) {
        return switch (getRecordingUseCase.execute(new GetRecordingQuery(
                io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.RecordingId.of(id)))) {
            case Result.Success<?, MeetingError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    @GetMapping(value = "/{version}/meetings/{id}/recordings", version = "1.0")
    public ResponseEntity<JsendResponse<?>> listMeetingRecordings(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) @Nullable String pageToken) {
        ListMeetingRecordingsQuery query = new ListMeetingRecordingsQuery(id, pageSize, pageToken);

        if (pageToken == null) {
            return executeListRecordings(query, null);
        }
        var decodeResult = cursorTokenEncoder.decode(pageToken);
        return switch (decodeResult) {
            case Result.Failure<ScrollCursor, CursorErrorCode> f ->
                ResponseEntity.badRequest()
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
            case Result.Success<ScrollCursor, CursorErrorCode> s ->
                executeListRecordings(query, s.value());
        };
    }

    private ResponseEntity<JsendResponse<?>> executeListRecordings(
            ListMeetingRecordingsQuery query, @Nullable ScrollCursor cursor) {
        var pageResult = listMeetingRecordingsUseCase.execute(query, cursor);
        String nextPageToken = null;
        if (pageResult.hasNext() && !pageResult.items().isEmpty()) {
            var last = pageResult.items().getLast();
            nextPageToken = cursorTokenEncoder.encode(last.createdAt(), last.id().value());
        }
        var response =
                new CursorScrollResponse<>(pageResult.items(), query.pageSize(), nextPageToken);
        return ResponseEntity.ok(JsendResponse.success(response));
    }
}
