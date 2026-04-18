package io.github.phunguy65.zms.domain.model;

import java.time.OffsetDateTime;
import java.util.List;

/** Full detail model for a meeting history item. */
public record MeetingHistoryDetail(
        String id,
        String hostId,
        String shortCode,
        String title,
        String description,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        MeetingType type,
        MeetingStatus status,
        OffsetDateTime createdAt,
        List<MeetingParticipant> participants,
        List<MeetingRecording> recordings) {

    public MeetingHistoryDetail {
        participants = List.copyOf(participants);
        recordings = List.copyOf(recordings);
    }
}
