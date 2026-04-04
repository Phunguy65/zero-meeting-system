package io.github.phunguy65.zms.meetingmanagement.presentation;

import io.github.phunguy65.zms.meetingmanagement.application.command.KickParticipantCommand;
import io.github.phunguy65.zms.meetingmanagement.application.query.GetParticipantsQuery;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.GetParticipantsUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.KickParticipantUseCase;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParticipantController extends BaseController {

    private final GetParticipantsUseCase getParticipantsUseCase;
    private final KickParticipantUseCase kickParticipantUseCase;

    public ParticipantController(
            GetParticipantsUseCase getParticipantsUseCase,
            KickParticipantUseCase kickParticipantUseCase) {
        this.getParticipantsUseCase = getParticipantsUseCase;
        this.kickParticipantUseCase = kickParticipantUseCase;
    }

    @GetMapping(value = "/{version}/meetings/{id}/participants", version = "1.0")
    public ResponseEntity<JsendResponse<?>> getParticipants(@PathVariable UUID id) {
        return switch (getParticipantsUseCase.execute(new GetParticipantsQuery(id))) {
            case Result.Success<?, MeetingError> s ->
                ResponseEntity.ok(JsendResponse.success(s.value()));
            case Result.Failure<?, MeetingError> f -> errorResponse(f.error());
        };
    }

    /**
     * Kicks an active participant from a live meeting. Only the meeting host can perform this action.
     *
     * <p>Exactly one of {@code userId} (registered participant) or {@code displayName} (guest) must
     * be provided.
     *
     * @param id           the meeting ID
     * @param userId       target registered participant's user ID, or null to target by displayName
     * @param displayName  target guest's display name, or null to target by userId
     * @param authentication the authenticated host
     * @return 204 No Content on success; 4xx/5xx error otherwise
     */
    @PostMapping(value = "/{version}/meetings/{id}/participants:kick", version = "1.0")
    public ResponseEntity<JsendResponse<?>> kickParticipant(
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
