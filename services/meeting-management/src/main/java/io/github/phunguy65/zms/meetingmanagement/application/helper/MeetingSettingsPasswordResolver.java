package io.github.phunguy65.zms.meetingmanagement.application.helper;

import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingSettings;
import io.github.phunguy65.zms.meetingmanagement.domain.port.PasswordHasher;
import org.jspecify.annotations.Nullable;

public final class MeetingSettingsPasswordResolver {

    private MeetingSettingsPasswordResolver() {}

    public static @Nullable String normalizeRawPassword(@Nullable String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return null;
        }
        return rawPassword;
    }

    public static MeetingSettings withRawPassword(
            MeetingSettings settings, @Nullable String rawPassword, PasswordHasher passwordHasher) {
        String normalizedRawPassword = normalizeRawPassword(rawPassword);
        String passwordHash =
                normalizedRawPassword != null ? passwordHasher.hash(normalizedRawPassword) : null;
        return new MeetingSettings(
                settings.admissionPolicy(),
                settings.joinRequestTimeout(),
                settings.allowGuest(),
                settings.muteOnEntry(),
                settings.maxParticipants(),
                settings.recordingEnabled(),
                settings.screenShareMode(),
                settings.chatEnabled(),
                passwordHash);
    }
}
