package io.github.phunguy65.zms.meetingmanagement.domain;

import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.RecordingStatus;
import io.github.phunguy65.zms.shared.domain.DomainError;
import java.util.UUID;

/** Domain errors for the meeting-management bounded context. */
public sealed interface MeetingError extends DomainError {

    record MeetingNotFound(UUID id) implements MeetingError {
        @Override
        public String message() {
            return "Meeting not found: " + id;
        }
    }

    record InvalidStatusTransition(MeetingStatus from, MeetingStatus to) implements MeetingError {
        @Override
        public String message() {
            return "Cannot transition meeting from " + from + " to " + to;
        }
    }

    record InvalidRecordingTransition(RecordingStatus from, RecordingStatus to)
            implements MeetingError {
        @Override
        public String message() {
            return "Cannot transition recording from " + from + " to " + to;
        }
    }

    record RecordingAlreadyActive(UUID meetingId) implements MeetingError {
        @Override
        public String message() {
            return "A recording is already active for meeting: " + meetingId;
        }
    }

    record RecordingNotFound(UUID id) implements MeetingError {
        @Override
        public String message() {
            return "Recording not found: " + id;
        }
    }

    record ShortCodeExhausted() implements MeetingError {
        @Override
        public String message() {
            return "Failed to generate a unique short code after maximum attempts";
        }
    }

    record NotAuthorized(UUID requesterId, UUID hostId) implements MeetingError {
        @Override
        public String message() {
            return "Requester " + requesterId + " is not the host of this meeting";
        }
    }

    record ParticipationLogNotFound(UUID meetingId, String deviceId) implements MeetingError {
        @Override
        public String message() {
            return "No active participation log for meeting " + meetingId + " device " + deviceId;
        }
    }
}
