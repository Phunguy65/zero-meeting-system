package io.github.phunguy65.zms.domain.usecase.meeting;

import io.github.phunguy65.zms.domain.model.MeetingSettings;
import io.github.phunguy65.zms.domain.model.UpdateSettingsResult;
import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * Use case for updating meeting settings.
 *
 * <p>Replaces meeting settings for SCHEDULED or LIVE meetings.
 * Used for host-only live meeting settings and pre-meeting settings edits.
 * Returns an {@link UpdateSettingsResult} that includes the updated settings and any
 * invite-token invalidation metadata caused by a password change.
 */
public class UpdateMeetingSettingsUseCase {

    private final MeetingRepository meetingRepository;

    @Inject
    public UpdateMeetingSettingsUseCase(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    /**
     * Updates meeting settings for a meeting.
     *
     * @param meetingId the meeting UUID
     * @param settings the new meeting settings to apply
     * @return a CompletableFuture that completes with the settings update result
     */
    public CompletableFuture<UpdateSettingsResult> execute(
            String meetingId, MeetingSettings settings) {
        return meetingRepository.updateMeetingSettings(meetingId, settings);
    }
}
