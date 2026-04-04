package io.github.phunguy65.zms.meetingmanagement.application.command;

import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.RecordingId;
import org.jspecify.annotations.Nullable;

public record CompleteRecordingCommand(
        RecordingId recordingId,
        String fileUrl,
        String storagePath,
        @Nullable String thumbnailUrl,
        int durationSeconds,
        long fileSizeBytes) {}
