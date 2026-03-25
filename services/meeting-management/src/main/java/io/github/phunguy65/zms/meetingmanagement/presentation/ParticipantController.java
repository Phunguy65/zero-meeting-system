package io.github.phunguy65.zms.meetingmanagement.presentation;

import io.github.phunguy65.zms.meetingmanagement.application.query.GetParticipantsQuery;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.GetParticipantsUseCase;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ParticipationLogCursor;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ParticipationLogId;
import io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence.ParticipationLogCursorEncoder;
import io.github.phunguy65.zms.shared.domain.CursorErrorCode;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.infrastructure.web.CursorScrollResponse;
import io.github.phunguy65.zms.shared.infrastructure.web.FailData;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParticipantController extends BaseController {

    private final GetParticipantsUseCase getParticipantsUseCase;
    private final ParticipationLogCursorEncoder participationLogCursorEncoder;

    public ParticipantController(
            GetParticipantsUseCase getParticipantsUseCase,
            ParticipationLogCursorEncoder participationLogCursorEncoder) {
        this.getParticipantsUseCase = getParticipantsUseCase;
        this.participationLogCursorEncoder = participationLogCursorEncoder;
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
            nextPageToken = participationLogCursorEncoder.encode(
                    last.joinedAt(), ParticipationLogId.of(last.id()));
        }
        var response =
                new CursorScrollResponse<>(pageResult.items(), query.pageSize(), nextPageToken);
        return ResponseEntity.ok(JsendResponse.success(response));
    }
}
