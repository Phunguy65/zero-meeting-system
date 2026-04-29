package io.github.phunguy65.zms.data.mapper;

import io.github.phunguy65.zms.data.remote.dto.MeetingManagementMeetingDetailResponse;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementMeetingParticipantResponse;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementMeetingResponse;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementMeetingSettingsResponse;
import io.github.phunguy65.zms.data.remote.dto.MeetingManagementRecordingResponse;
import io.github.phunguy65.zms.domain.model.CalendarEvent;
import io.github.phunguy65.zms.domain.model.MeetingCreationResult;
import io.github.phunguy65.zms.domain.model.MeetingDetail;
import io.github.phunguy65.zms.domain.model.MeetingHistory;
import io.github.phunguy65.zms.domain.model.MeetingHistoryDetail;
import io.github.phunguy65.zms.domain.model.MeetingParticipant;
import io.github.phunguy65.zms.domain.model.MeetingRecording;
import io.github.phunguy65.zms.domain.model.MeetingSettings;
import io.github.phunguy65.zms.domain.model.MeetingStatus;
import io.github.phunguy65.zms.domain.model.MeetingType;
import io.github.phunguy65.zms.domain.model.UpcomingMeeting;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Maps meeting DTOs from the remote API to domain models.
 * Handles meeting responses, settings, and related data structures.
 */
public class MeetingMapper {

    private static final String ADMISSION_POLICY_WAITING_ROOM = "MANUAL_APPROVAL";

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

    public MeetingHistoryDetail toMeetingHistoryDetail(
            MeetingManagementMeetingDetailResponse source) {
        List<MeetingParticipant> participants = safeList(source.getParticipants()).stream()
                .filter(Objects::nonNull)
                .map(this::toMeetingParticipant)
                .collect(Collectors.toList());

        List<MeetingRecording> recordings = safeList(source.getRecordings()).stream()
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

    /**
     * Maps a meeting response to a meeting creation result.
     * Used after instant or scheduled meeting creation.
     */
    public MeetingCreationResult toMeetingCreationResult(MeetingManagementMeetingResponse source) {
        return new MeetingCreationResult(
                uuidToString(source.getId()),
                source.getShortCode(),
                source.getTitle(),
                mapMeetingType(source.getType()),
                mapMeetingStatus(source.getStatus()),
                source.getStartTime(),
                source.getEndTime(),
                source.getCreatedAt());
    }

    /**
     * Maps a meeting response to an upcoming meeting for the dashboard.
     * Used by MeetingRepositoryImpl.getUpcomingHostMeetings().
     */
    public UpcomingMeeting toUpcomingMeeting(MeetingManagementMeetingResponse source) {
        return new UpcomingMeeting(
                uuidToString(source.getId()),
                source.getShortCode(),
                source.getTitle(),
                source.getStartTime(),
                source.getEndTime(),
                mapMeetingStatus(source.getStatus()));
    }

    /**
     * Maps a meeting response to a calendar event for the calendar view.
     * Used by CalendarRepositoryImpl.getEventsForDateRange().
     */
    public CalendarEvent toCalendarEvent(MeetingManagementMeetingResponse source) {
        return new CalendarEvent(
                uuidToString(source.getId()),
                source.getTitle(),
                source.getStartTime(),
                source.getEndTime(),
                mapMeetingStatus(source.getStatus()),
                mapMeetingType(source.getType()));
    }

    /**
     * Maps a meeting response to a meeting detail for edit mode and settings display.
     * Includes meeting metadata and current settings.
     */
    public MeetingDetail toMeetingDetail(MeetingManagementMeetingResponse source) {
        MeetingSettings settings = toMeetingSettings(source.getSettings());
        return new MeetingDetail(
                uuidToString(source.getId()),
                uuidToString(source.getHostId()),
                source.getShortCode(),
                source.getTitle(),
                source.getStartTime(),
                source.getEndTime(),
                mapMeetingStatus(source.getStatus()),
                mapMeetingType(source.getType()),
                settings);
    }

    /**
     * Maps a settings response to the domain MeetingSettings model.
     * Handles null source by returning defaults.
     */
    public MeetingSettings toMeetingSettings(MeetingManagementMeetingSettingsResponse source) {
        if (source == null) {
            return MeetingSettings.defaults();
        }

        boolean waitingRoomEnabled =
                ADMISSION_POLICY_WAITING_ROOM.equals(source.getAdmissionPolicy());
        boolean requirePassword =
                source.getRequirePassword() != null ? source.getRequirePassword() : false;

        return new MeetingSettings.Builder()
                .waitingRoomEnabled(waitingRoomEnabled)
                .allowGuest(source.getAllowGuest() != null ? source.getAllowGuest() : true)
                .requirePassword(requirePassword)
                .maxParticipants(
                        source.getMaxParticipants() != null ? source.getMaxParticipants() : 100)
                .allowScreenShare(
                        source.getAllowScreenShare() != null ? source.getAllowScreenShare() : true)
                .chatEnabled(source.getChatEnabled() != null ? source.getChatEnabled() : true)
                .allowMicrophone(
                        source.getAllowMicrophone() != null ? source.getAllowMicrophone() : true)
                .allowVideo(source.getAllowVideo() != null ? source.getAllowVideo() : true)
                .build();
    }

    private MeetingParticipant toMeetingParticipant(
            MeetingManagementMeetingParticipantResponse source) {
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

    private MeetingStatus mapMeetingStatus(
            MeetingManagementMeetingDetailResponse.StatusEnum status) {
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
