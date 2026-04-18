package io.github.phunguy65.zms.data.mapper;

import io.github.phunguy65.zms.data.remote.dto.MeetingManagementMeetingDetailResponse;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementMeetingParticipantResponse;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementMeetingResponse;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementRecordingResponse;
import io.github.phunguy65.zms.domain.model.MeetingHistory;
import io.github.phunguy65.zms.domain.model.MeetingHistoryDetail;
import io.github.phunguy65.zms.domain.model.MeetingParticipant;
import io.github.phunguy65.zms.domain.model.MeetingRecording;
import io.github.phunguy65.zms.domain.model.MeetingStatus;
import io.github.phunguy65.zms.domain.model.MeetingType;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

/** Maps meeting history DTOs from the remote API to domain models. */
public class MeetingMapper {

    @Inject
    public MeetingMapper() {}

    public MeetingHistory toMeetingHistory(MeetingManagementMeetingResponse source) {
        return new MeetingHistory(
                uuidToString(source.getId()),
                source.getTitle(),
                source.getDescription(),
                source.getStartTime(),
                source.getEndTime(),
                mapMeetingType(source.getType()),
                mapMeetingStatus(source.getStatus()));
    }

    public MeetingHistoryDetail toMeetingHistoryDetail(MeetingManagementMeetingDetailResponse source) {
        List<MeetingParticipant> participants =
                safeList(source.getParticipants()).stream()
                        .filter(Objects::nonNull)
                        .map(this::toMeetingParticipant)
                        .collect(Collectors.toList());

        List<MeetingRecording> recordings =
                safeList(source.getRecordings()).stream()
                        .filter(Objects::nonNull)
                        .map(this::toMeetingRecording)
                        .collect(Collectors.toList());

        return new MeetingHistoryDetail(
                uuidToString(source.getId()),
                uuidToString(source.getHostId()),
                source.getShortCode(),
                source.getTitle(),
                source.getDescription(),
                source.getStartTime(),
                source.getEndTime(),
                mapMeetingType(source.getType()),
                mapMeetingStatus(source.getStatus()),
                source.getCreatedAt(),
                participants,
                recordings);
    }

    private MeetingParticipant toMeetingParticipant(MeetingManagementMeetingParticipantResponse source) {
        return new MeetingParticipant(
                uuidToString(source.getUserId()),
                source.getDisplayName(),
                source.getRole() != null ? source.getRole().getValue() : null,
                source.getJoinedAt(),
                source.getLeftAt());
    }

    private MeetingRecording toMeetingRecording(MeetingManagementRecordingResponse source) {
        return new MeetingRecording(
                uuidToString(source.getId()),
                source.getFileUrl(),
                source.getDurationSeconds(),
                source.getCreatedAt());
    }

    private MeetingType mapMeetingType(MeetingManagementMeetingResponse.TypeEnum type) {
        if (type == null) {
            return null;
        }
        return MeetingType.valueOf(type.getValue());
    }

    private MeetingType mapMeetingType(MeetingManagementMeetingDetailResponse.TypeEnum type) {
        if (type == null) {
            return null;
        }
        return MeetingType.valueOf(type.getValue());
    }

    private MeetingStatus mapMeetingStatus(MeetingManagementMeetingResponse.StatusEnum status) {
        if (status == null) {
            return null;
        }
        return MeetingStatus.valueOf(status.getValue());
    }

    private MeetingStatus mapMeetingStatus(MeetingManagementMeetingDetailResponse.StatusEnum status) {
        if (status == null) {
            return null;
        }
        return MeetingStatus.valueOf(status.getValue());
    }

    private String uuidToString(UUID value) {
        return value != null ? value.toString() : null;
    }

    private <T> List<T> safeList(List<T> items) {
        return items != null ? items : Collections.emptyList();
    }
}
