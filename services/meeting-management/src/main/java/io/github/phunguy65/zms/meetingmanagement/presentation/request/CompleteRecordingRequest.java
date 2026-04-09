package io.github.phunguy65.zms.meetingmanagement.presentation.request;

import io.github.phunguy65.zms.meetingmanagement.application.command.CompleteRecordingCommand;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.RecordingId;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record CompleteRecordingRequest(
        @NotBlank String fileUrl,
        @NotBlank String storagePath,
        @Nullable String thumbnailUrl,
        @Min(0) int durationSeconds,
        @Min(0) long fileSizeBytes) {

    public CompleteRecordingCommand toCommand(UUID recordingId) {
        return new CompleteRecordingCommand(
                RecordingId.of(recordingId),
                fileUrl,
                storagePath,
                thumbnailUrl,
                durationSeconds,
                fileSizeBytes);
    }
}
