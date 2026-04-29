package io.github.phunguy65.zms.presentation.meeting.participant;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.model.Participant;
import io.github.phunguy65.zms.domain.model.ParticipantRole;
import io.github.phunguy65.zms.domain.model.ParticipantRoleInfo;
import io.github.phunguy65.zms.domain.model.VideoParticipant;
import io.github.phunguy65.zms.domain.repository.ParticipantRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.inject.Inject;

/**
 * ViewModel for the participants bottom sheet.
 * Combines LiveKit real-time presence with backend role metadata
 * to produce a merged participant list for UI consumption.
 */
@HiltViewModel
public class ParticipantsViewModel extends ViewModel {

    private final ParticipantRepository participantRepository;
    private final Executor mainExecutor;

    private final MutableLiveData<List<Participant>> _participants =
            new MutableLiveData<>(new ArrayList<>());

    private List<VideoParticipant> cachedLiveKitParticipants = new ArrayList<>();
    private Map<String, ParticipantRole> roleByIdentity = new HashMap<>();
    private Map<String, ParticipantRole> roleByDisplayName = new HashMap<>();

    @Inject
    public ParticipantsViewModel(
            ParticipantRepository participantRepository, @MainExecutor Executor mainExecutor) {
        this.participantRepository = participantRepository;
        this.mainExecutor = mainExecutor;
    }

    public LiveData<List<Participant>> getParticipants() {
        return _participants;
    }

    /**
     * Receives LiveKit participants from CallViewModel and publishes a merged list.
     */
    public void setLiveKitParticipants(List<VideoParticipant> videoParticipants) {
        cachedLiveKitParticipants =
                videoParticipants != null ? new ArrayList<>(videoParticipants) : new ArrayList<>();
        publishMergedList();
    }

    /**
     * Fetches participant roles from the backend once per sheet session
     * and merges them into the current list.
     */
    public void enrichWithRoles(String meetingId) {
        if (meetingId == null || meetingId.isEmpty()) return;

        participantRepository
                .getParticipantRoles(meetingId)
                .whenCompleteAsync(
                        (roleInfos, error) -> {
                            if (error != null) {
                                publishMergedList();
                                return;
                            }

                            roleByIdentity = new HashMap<>();
                            roleByDisplayName = new HashMap<>();

                            if (roleInfos != null) {
                                for (ParticipantRoleInfo info : roleInfos) {
                                    if (info.getId() != null && !info.getId().isEmpty()) {
                                        roleByIdentity.put(info.getId(), info.getRole());
                                    }
                                    if (info.getDisplayName() != null
                                            && !info.getDisplayName().isEmpty()) {
                                        roleByDisplayName.put(
                                                info.getDisplayName(), info.getRole());
                                    }
                                }
                            }

                            publishMergedList();
                        },
                        mainExecutor);
    }

    private void publishMergedList() {
        List<Participant> merged = new ArrayList<>();
        for (VideoParticipant vp : cachedLiveKitParticipants) {
            merged.add(toParticipant(vp));
        }
        _participants.postValue(merged);
    }

    private Participant toParticipant(VideoParticipant vp) {
        ParticipantRole role = resolveRole(vp);
        return new Participant(
                vp.getId(),
                vp.getDisplayName(),
                role,
                vp.isMicEnabled(),
                vp.isCameraEnabled(),
                vp.isLocal());
    }

    private ParticipantRole resolveRole(VideoParticipant vp) {
        if (vp.getId() != null && roleByIdentity.containsKey(vp.getId())) {
            return roleByIdentity.get(vp.getId());
        }
        if (vp.getDisplayName() != null && roleByDisplayName.containsKey(vp.getDisplayName())) {
            return roleByDisplayName.get(vp.getDisplayName());
        }
        return ParticipantRole.PARTICIPANT;
    }
}
