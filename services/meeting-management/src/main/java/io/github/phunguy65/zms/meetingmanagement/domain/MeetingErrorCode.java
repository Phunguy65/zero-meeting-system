package io.github.phunguy65.zms.meetingmanagement.domain;

import io.github.phunguy65.zms.shared.domain.ErrorCode;

/** Machine-readable error codes for JSend {@code fail} responses. */
public enum MeetingErrorCode implements ErrorCode {
    MEETING_NOT_FOUND,
    INVALID_STATUS_TRANSITION,
    INVALID_RECORDING_TRANSITION,
    RECORDING_ALREADY_ACTIVE,
    RECORDING_NOT_FOUND,
    SHORT_CODE_EXHAUSTED,
    NOT_AUTHORIZED,
    PARTICIPATION_LOG_NOT_FOUND
}
