package io.github.phunguy65.zms.meetingmanagement.presentation;

import io.github.phunguy65.zms.meetingmanagement.application.query.GetParticipatedMeetingDetailQuery;
import io.github.phunguy65.zms.meetingmanagement.application.query.GetParticipatedMeetingsQuery;
import io.github.phunguy65.zms.meetingmanagement.application.response.MeetingDetailResponse;
import io.github.phunguy65.zms.meetingmanagement.application.response.MeetingResponse;
import io.github.phunguy65.zms.meetingmanagement.application.response.ParticipatedMeetingPageResponse;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.GetParticipatedMeetingDetailUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.GetParticipatedMeetingsUseCase;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ParticipatedMeetingCursor;
import io.github.phunguy65.zms.meetingmanagement.infrastructure.web.ParticipatedMeetingCursorCodec;
import io.github.phunguy65.zms.shared.domain.CursorErrorCode;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.infrastructure.web.CommonErrorCode;
import io.github.phunguy65.zms.shared.infrastructure.web.CursorScrollResponse;
import io.github.phunguy65.zms.shared.infrastructure.web.FailData;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "User Meetings", description = "User participated meetings")
public class UserMeetingController extends BaseController {

    private final GetParticipatedMeetingsUseCase getParticipatedMeetingsUseCase;
    private final GetParticipatedMeetingDetailUseCase getParticipatedMeetingDetailUseCase;
    private final ParticipatedMeetingCursorCodec participatedMeetingCursorCodec;

    public UserMeetingController(
            GetParticipatedMeetingsUseCase getParticipatedMeetingsUseCase,
            GetParticipatedMeetingDetailUseCase getParticipatedMeetingDetailUseCase,
            ParticipatedMeetingCursorCodec participatedMeetingCursorCodec) {
        this.getParticipatedMeetingsUseCase = getParticipatedMeetingsUseCase;
        this.getParticipatedMeetingDetailUseCase = getParticipatedMeetingDetailUseCase;
        this.participatedMeetingCursorCodec = participatedMeetingCursorCodec;
    }

    @Operation(summary = "List participated meetings with optional status filter")
    @SuppressWarnings("unchecked")
    @GetMapping(value = "/{version}/users/{userId}/meetings:filter", version = "1.0")
    public ResponseEntity<JsendResponse<CursorScrollResponse<MeetingResponse>>>
            listParticipatedMeetings(
                    @PathVariable UUID userId,
                    @RequestParam(defaultValue = "20") int pageSize,
                    @RequestParam(required = false) @Nullable String pageToken,
                    @RequestParam(required = false) @Nullable String status,
                    Authentication auth) {
        UUID requesterId = extractUserId(auth);
        if (requesterId == null) return unauthenticated();

        var statuses = parseStatuses(status);
        if (statuses == null) return invalidStatuses();

        if (pageToken == null) {
            return executeListMeetings(new GetParticipatedMeetingsQuery(
                    userId, requesterId, statuses, pageSize, null));
        }

        return switch (participatedMeetingCursorCodec.decode(pageToken)) {
            case Result.Failure<ParticipatedMeetingCursor, CursorErrorCode> f ->
                (ResponseEntity<JsendResponse<CursorScrollResponse<MeetingResponse>>>)
                        (ResponseEntity<?>) ResponseEntity.badRequest()
                                .body(JsendResponse.fail(
                                        new FailData(f.error().name(), f.error(), List.of())));
            case Result.Success<ParticipatedMeetingCursor, CursorErrorCode> s ->
                executeListMeetings(new GetParticipatedMeetingsQuery(
                        userId, requesterId, statuses, pageSize, s.value()));
        };
    }

    @Operation(summary = "Get participated meeting detail")
    @GetMapping(value = "/{version}/users/{userId}/meetings/{meetingId}", version = "1.0")
    public ResponseEntity<JsendResponse<MeetingDetailResponse>> getParticipatedMeetingDetail(
            @PathVariable UUID userId, @PathVariable UUID meetingId, Authentication auth) {
        UUID requesterId = extractUserId(auth);
        if (requesterId == null) return unauthenticated();

        return switch (getParticipatedMeetingDetailUseCase.execute(
                new GetParticipatedMeetingDetailQuery(userId, meetingId, requesterId))) {
            case Result.Success<MeetingDetailResponse, MeetingError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<MeetingDetailResponse, MeetingError> f -> errorResponse(f.error());
        };
    }

    private ResponseEntity<JsendResponse<CursorScrollResponse<MeetingResponse>>>
            executeListMeetings(GetParticipatedMeetingsQuery query) {
        return switch (getParticipatedMeetingsUseCase.execute(query)) {
            case Result.Success<ParticipatedMeetingPageResponse, MeetingError> s -> {
                var page = s.value();
                String nextPageToken = null;
                if (page.hasNext() && !page.items().isEmpty()) {
                    var last = page.items().getLast();
                    nextPageToken =
                            participatedMeetingCursorCodec.encode(new ParticipatedMeetingCursor(
                                    last.lastJoinedAt(), last.meeting().id()));
                }
                yield ResponseEntity.ok(JsendResponse.success(new CursorScrollResponse<>(
                        page.items().stream().map(item -> item.meeting()).toList(),
                        page.pageSize(),
                        nextPageToken)));
            }
            case Result.Failure<ParticipatedMeetingPageResponse, MeetingError> f ->
                errorResponse(f.error());
        };
    }

    private @Nullable Set<MeetingStatus> parseStatuses(@Nullable String rawStatuses) {
        if (rawStatuses == null || rawStatuses.isBlank()) return Set.of();
        try {
            return Arrays.stream(rawStatuses.split(","))
                    .map(String::trim)
                    .filter(token -> !token.isEmpty())
                    .map(MeetingStatus::valueOf)
                    .collect(Collectors.toSet());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<JsendResponse<T>> invalidStatuses() {
        return (ResponseEntity<JsendResponse<T>>) (ResponseEntity<?>) ResponseEntity.badRequest()
                .body(JsendResponse.fail(new FailData(
                        "Invalid status filter", CommonErrorCode.VALIDATION_ERROR, List.of())));
    }
}
