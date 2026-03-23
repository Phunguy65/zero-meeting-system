package io.github.phunguy65.zms.meetingmanagement.presentation;

import io.github.phunguy65.zms.meetingmanagement.application.query.GetParticipantsQuery;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.GetParticipantsUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.LeaveMeetingUseCase;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ParticipationLogCursor;
import io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence.ParticipationLogCursorEncoder;
import io.github.phunguy65.zms.meetingmanagement.presentation.request.LeaveMeetingRequest;
import io.github.phunguy65.zms.shared.domain.CursorErrorCode;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.infrastructure.web.CursorScrollResponse;
import io.github.phunguy65.zms.shared.infrastructure.web.FailData;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class ParticipantController extends BaseController {

    private final LeaveMeetingUseCase leaveMeetingUseCase;
    private final GetParticipantsUseCase getParticipantsUseCase;
    private final ParticipationLogCursorEncoder participationLogCursorEncoder;

    public ParticipantController(
            LeaveMeetingUseCase leaveMeetingUseCase,
            GetParticipantsUseCase getParticipantsUseCase,
            ParticipationLogCursorEncoder participationLogCursorEncoder) {
        this.leaveMeetingUseCase = leaveMeetingUseCase;
        this.getParticipantsUseCase = getParticipantsUseCase;
        this.participationLogCursorEncoder = participationLogCursorEncoder;
    }

    @PostMapping(value = "/{version}/meetings/{id}:leave", version = "1.0")
    public ResponseEntity<JsendResponse<?>> leaveMeeting(
            @PathVariable UUID id, @Valid @RequestBody LeaveMeetingRequest request) {
        return switch (leaveMeetingUseCase.execute(request.toCommand(id))) {
            case Result.Success<?, MeetingError> s -> ResponseEntity.ok(JsendResponse.success());
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    @GetMapping(value = "/{version}/meetings/{id}/participants", version = "1.0")
    public ResponseEntity<JsendResponse<?>> getParticipants(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) @Nullable String pageToken) {
        GetParticipantsQuery query = new GetParticipantsQuery(id, pageSize, pageToken);

        if (pageToken == null) {
            return executeGetParticipants(query, null);
        }
        var decodeResult = participationLogCursorEncoder.decode(pageToken);
        return switch (decodeResult) {
            case Result.Failure<ParticipationLogCursor, CursorErrorCode> f ->
                ResponseEntity.badRequest()
                        .body(JsendResponse.fail(
                                new FailData(f.error().name(), f.error(), List.of())));
            case Result.Success<ParticipationLogCursor, CursorErrorCode> s ->
                executeGetParticipants(query, s.value());
        };
    }

    private ResponseEntity<JsendResponse<?>> executeGetParticipants(
            GetParticipantsQuery query, @Nullable ParticipationLogCursor cursor) {
        var pageResult = getParticipantsUseCase.execute(query, cursor);
        String nextPageToken = null;
        if (pageResult.hasNext() && !pageResult.items().isEmpty()) {
            var last = pageResult.items().getLast();
            nextPageToken = participationLogCursorEncoder.encode(last.joinedAt(), last.id());
        }
        var response =
                new CursorScrollResponse<>(pageResult.items(), query.pageSize(), nextPageToken);
        return ResponseEntity.ok(JsendResponse.success(response));
    }
}
