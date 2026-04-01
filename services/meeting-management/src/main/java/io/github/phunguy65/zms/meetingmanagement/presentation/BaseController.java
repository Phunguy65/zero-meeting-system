package io.github.phunguy65.zms.meetingmanagement.presentation;

import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingErrorCode;
import io.github.phunguy65.zms.shared.infrastructure.web.FailData;
import io.github.phunguy65.zms.shared.infrastructure.web.JsendResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

/**
 * Shared controller base that centralises {@link MeetingError} → HTTP status and error code
 * mapping for all meeting-management controllers.
 */
abstract class BaseController {

    protected ResponseEntity<JsendResponse<?>> errorResponse(MeetingError error) {
        HttpStatus status =
                switch (error) {
                    case MeetingError.MeetingNotFound e -> HttpStatus.NOT_FOUND;
                    case MeetingError.MeetingNotFoundByShortCode e -> HttpStatus.NOT_FOUND;
                    case MeetingError.RecordingNotFound e -> HttpStatus.NOT_FOUND;
                    case MeetingError.ParticipationLogNotFound e -> HttpStatus.NOT_FOUND;
                    case MeetingError.JoinRequestNotFound e -> HttpStatus.NOT_FOUND;
                    case MeetingError.InvalidStatusTransition e -> HttpStatus.CONFLICT;
                    case MeetingError.InvalidRecordingTransition e -> HttpStatus.CONFLICT;
                    case MeetingError.RecordingAlreadyActive e -> HttpStatus.CONFLICT;
                    case MeetingError.NoActiveRecording e -> HttpStatus.CONFLICT;
                    case MeetingError.MeetingFull e -> HttpStatus.CONFLICT;
                    case MeetingError.InvalidJoinRequestTransition e -> HttpStatus.CONFLICT;
                    case MeetingError.NotAuthorized e -> HttpStatus.FORBIDDEN;
                    case MeetingError.GuestNotAllowed e -> HttpStatus.FORBIDDEN;
                    case MeetingError.ShortCodeExhausted e -> HttpStatus.SERVICE_UNAVAILABLE;
                    case MeetingError.LiveKitUnavailable e -> HttpStatus.SERVICE_UNAVAILABLE;
                    case MeetingError.LiveKitParticipantNotFound e ->
                        HttpStatus.SERVICE_UNAVAILABLE;
                    case MeetingError.UserServiceUnavailable e -> HttpStatus.SERVICE_UNAVAILABLE;
                    case MeetingError.InviteeNotFound e -> HttpStatus.UNPROCESSABLE_CONTENT;
                    case MeetingError.InvalidMeetingDuration e -> HttpStatus.BAD_REQUEST;
                    case MeetingError.InvalidSettings e -> HttpStatus.BAD_REQUEST;
                    case MeetingError.InvalidInviteeTransition e -> HttpStatus.CONFLICT;
                    case MeetingError.InvalidPassword e -> HttpStatus.UNAUTHORIZED;
                    case MeetingError.JoinRequestExpired e -> HttpStatus.GONE;
                    case MeetingError.NotWaitingForApproval e -> HttpStatus.UNPROCESSABLE_CONTENT;
                };
        MeetingErrorCode code =
                switch (error) {
                    case MeetingError.MeetingNotFound e -> MeetingErrorCode.MEETING_NOT_FOUND;
                    case MeetingError.MeetingNotFoundByShortCode e ->
                        MeetingErrorCode.MEETING_NOT_FOUND;
                    case MeetingError.RecordingNotFound e -> MeetingErrorCode.RECORDING_NOT_FOUND;
                    case MeetingError.ParticipationLogNotFound e ->
                        MeetingErrorCode.PARTICIPATION_LOG_NOT_FOUND;
                    case MeetingError.InvalidStatusTransition e ->
                        MeetingErrorCode.INVALID_STATUS_TRANSITION;
                    case MeetingError.InvalidRecordingTransition e ->
                        MeetingErrorCode.INVALID_RECORDING_TRANSITION;
                    case MeetingError.RecordingAlreadyActive e ->
                        MeetingErrorCode.RECORDING_ALREADY_ACTIVE;
                    case MeetingError.NoActiveRecording e -> MeetingErrorCode.NO_ACTIVE_RECORDING;
                    case MeetingError.MeetingFull e -> MeetingErrorCode.MEETING_FULL;
                    case MeetingError.ShortCodeExhausted e -> MeetingErrorCode.SHORT_CODE_EXHAUSTED;
                    case MeetingError.LiveKitUnavailable e -> MeetingErrorCode.LIVEKIT_UNAVAILABLE;
                    case MeetingError.LiveKitParticipantNotFound e ->
                        MeetingErrorCode.LIVEKIT_UNAVAILABLE;
                    case MeetingError.UserServiceUnavailable e ->
                        MeetingErrorCode.USER_SERVICE_UNAVAILABLE;
                    case MeetingError.InviteeNotFound e -> MeetingErrorCode.INVITEE_NOT_FOUND;
                    case MeetingError.InvalidMeetingDuration e ->
                        MeetingErrorCode.INVALID_MEETING_DURATION;
                    case MeetingError.InvalidSettings e -> MeetingErrorCode.INVALID_SETTINGS;
                    case MeetingError.InvalidInviteeTransition e ->
                        MeetingErrorCode.INVALID_INVITEE_TRANSITION;
                    case MeetingError.InvalidPassword e -> MeetingErrorCode.INVALID_PASSWORD;
                    case MeetingError.GuestNotAllowed e -> MeetingErrorCode.GUEST_NOT_ALLOWED;
                    case MeetingError.NotAuthorized e -> MeetingErrorCode.NOT_AUTHORIZED;
                    case MeetingError.JoinRequestNotFound e ->
                        MeetingErrorCode.JOIN_REQUEST_NOT_FOUND;
                    case MeetingError.JoinRequestExpired e -> MeetingErrorCode.JOIN_REQUEST_EXPIRED;
                    case MeetingError.InvalidJoinRequestTransition e ->
                        MeetingErrorCode.INVALID_JOIN_REQUEST_TRANSITION;
                    case MeetingError.NotWaitingForApproval e ->
                        MeetingErrorCode.NOT_WAITING_FOR_APPROVAL;
                };
        return ResponseEntity.status(status)
                .body(JsendResponse.fail(new FailData(error.message(), code, List.of())));
    }

    protected UUID extractUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof String principalId)) {
            return null;
        }
        try {
            return UUID.fromString(principalId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    protected ResponseEntity<JsendResponse<?>> unauthenticated() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(JsendResponse.error("Authentication required"));
    }
}
