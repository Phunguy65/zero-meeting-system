package io.github.phunguy65.zms.meetingmanagement.domain.projection;

import org.jspecify.annotations.Nullable;

public record MeetingSettingsSummary(
        String admissionPolicy,
        @Nullable Integer joinRequestTimeoutSeconds,
        boolean allowGuest,
        boolean muteOnEntry,
        int maxParticipants,
        boolean recordingEnabled,
        String screenShareMode,
        boolean chatEnabled,
        boolean passwordProtected) {}
