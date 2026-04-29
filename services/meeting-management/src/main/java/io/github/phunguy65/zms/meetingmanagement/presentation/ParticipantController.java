package io.github.phunguy65.zms.meetingmanagement.presentation;

import io.github.phunguy65.zms.meetingmanagement.application.command.KickParticipantCommand;
import io.github.phunguy65.zms.meetingmanagement.application.query.GetParticipantsQuery;
import io.github.phunguy65.zms.meetingmanagement.application.response.ParticipantListItemResponse;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.GetParticipantsUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.KickParticipantUseCase;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Participants", description = "Meeting participant management")
public class ParticipantController extends BaseController {

    private final GetParticipantsUseCase getParticipantsUseCase;
    private final KickParticipantUseCase kickParticipantUseCase;

    public ParticipantController(
            GetParticipantsUseCase getParticipantsUseCase,
            KickParticipantUseCase kickParticipantUseCase) {
        this.getParticipantsUseCase = getParticipantsUseCase;
        this.kickParticipantUseCase = kickParticipantUseCase;
    }

    @Operation(summary = "List participants of a meeting")
    @GetMapping(value = "/{version}/meetings/{id}/participants", version = "1.0")
    public ResponseEntity<JsendResponse<List<ParticipantListItemResponse>>> getParticipants(
            @PathVariable UUID id) {
        return switch (getParticipantsUseCase.execute(new GetParticipantsQuery(id))) {
            case Result.Success<List<ParticipantListItemResponse>, MeetingError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<List<ParticipantListItemResponse>, MeetingError> f ->
                errorResponse(f.error());
        };
    }

    /**
     * Kicks an active participant from a live meeting. Only the meeting host can perform this action.
     *
     * <p>Exactly one of {@code userId} (registered participant) or {@code displayName} (guest) must
     * be provided.
     */
    @Operation(summary = "Kick a participant from a meeting")
    @PostMapping(value = "/{version}/meetings/{id}/participants:kick", version = "1.0")
    public ResponseEntity<JsendResponse<Void>> kickParticipant(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String displayName,
            Authentication authentication) {
        UUID requesterId = extractUserId(authentication);
        if (requesterId == null) {
            return unauthenticated();
        }

        var command = new KickParticipantCommand(id, requesterId, userId, displayName);
        return switch (kickParticipantUseCase.execute(command)) {
            case Result.Success<Void, MeetingError> _ ->
                ResponseEntity.noContent().build();
            case Result.Failure<Void, MeetingError> f -> errorResponse(f.error());
        };
    }
}
