package io.github.phunguy65.zms.meetingmanagement.domain.model;

public enum RecordingStatus {
    /** Đang ghi */
    RECORDING,
    /** Đang ghép file / upload */
    PROCESSING,
    /** Đã có URL */
    COMPLETED,
    FAILED;

    public boolean canTransitionTo(RecordingStatus target) {
        return switch (this) {
            case RECORDING -> target == PROCESSING || target == FAILED;
            case PROCESSING -> target == COMPLETED || target == FAILED;
            case COMPLETED, FAILED -> false;
        };
    }
}
