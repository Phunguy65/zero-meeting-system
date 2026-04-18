package io.github.phunguy65.zms.presentation.main.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.domain.model.MeetingHistoryDetail;
import io.github.phunguy65.zms.domain.model.MeetingParticipant;
import io.github.phunguy65.zms.domain.model.MeetingRecording;
import io.github.phunguy65.zms.domain.model.MeetingStatus;
import io.github.phunguy65.zms.domain.model.MeetingType;
import io.github.phunguy65.zms.frontends.R;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment showing the full detail of a historical meeting, including participants and recordings.
 *
 * <p>Supports in-app playback of recordings with Media3 ExoPlayer. Player lifecycle is scoped to
 * the view: paused in {@link #onPause()}, resumed in {@link #onResume()} if it was playing, and
 * released in {@link #onDestroyView()}. Playback position survives configuration changes via
 * saved-instance state.
 */
@AndroidEntryPoint
public class MeetingDetailFragment extends Fragment {

    private static final int PARTICIPANTS_PREVIEW_LIMIT = 5;

    private static final String STATE_PLAYER_POSITION = "player_position";
    private static final String STATE_PLAYER_WINDOW = "player_window";
    private static final String STATE_PLAYER_PLAY_WHEN_READY = "player_play_when_ready";
    private static final String STATE_CURRENT_RECORDING_URL = "current_recording_url";
    private static final String STATE_PARTICIPANTS_EXPANDED = "participants_expanded";

    private MeetingDetailViewModel viewModel;

    private ProgressBar progressLoading;
    private View layoutErrorState;
    private TextView tvErrorMessage;
    private MaterialButton btnRetry;
    private ScrollView scrollContent;

    private TextView tvTitle;
    private TextView tvTypeBadge;
    private TextView tvStatusBadge;
    private TextView tvDate;
    private TextView tvTimeRange;
    private LinearLayout sectionDescription;
    private TextView tvDescription;
    private TextView tvParticipantsHeader;
    private RecyclerView recyclerParticipants;
    private TextView btnExpandParticipants;
    private LinearLayout sectionRecordings;
    private TextView tvRecordingsHeader;
    private RecyclerView recyclerRecordings;

    private FrameLayout playerOverlay;
    private PlayerView playerView;
    private FrameLayout btnClosePlayerWrapper;
    private ImageView btnClosePlayer;
    private View layoutPlayerError;
    private MaterialButton btnPlayerRetry;
    private ImageView btnBack;
    private View btnBackWrapper;

    private ParticipantDetailAdapter participantAdapter;
    private RecordingAdapter recordingAdapter;

    @Nullable private MeetingHistoryDetail currentDetail;
    private boolean participantsExpanded;

    @Nullable private ExoPlayer player;
    @Nullable private String currentRecordingUrl;
    private long savedPlayerPosition = 0L;
    private int savedPlayerWindow = 0;
    private boolean savedPlayWhenReady = true;

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meeting_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MeetingDetailViewModel.class);

        initViews(view);
        setupRecyclers();
        setupListeners();
        restoreState(savedInstanceState);
        observeViewModel();
    }

    private void initViews(View view) {
        progressLoading = view.findViewById(R.id.progressLoading);
        layoutErrorState = view.findViewById(R.id.layoutErrorState);
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage);
        btnRetry = view.findViewById(R.id.btnRetry);
        scrollContent = view.findViewById(R.id.scrollContent);

        tvTitle = view.findViewById(R.id.tvTitle);
        tvTypeBadge = view.findViewById(R.id.tvTypeBadge);
        tvStatusBadge = view.findViewById(R.id.tvStatusBadge);
        tvDate = view.findViewById(R.id.tvDate);
        tvTimeRange = view.findViewById(R.id.tvTimeRange);
        sectionDescription = view.findViewById(R.id.sectionDescription);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvParticipantsHeader = view.findViewById(R.id.tvParticipantsHeader);
        recyclerParticipants = view.findViewById(R.id.recyclerParticipants);
        btnExpandParticipants = view.findViewById(R.id.btnExpandParticipants);
        sectionRecordings = view.findViewById(R.id.sectionRecordings);
        tvRecordingsHeader = view.findViewById(R.id.tvRecordingsHeader);
        recyclerRecordings = view.findViewById(R.id.recyclerRecordings);

        playerOverlay = view.findViewById(R.id.playerOverlay);
        playerView = view.findViewById(R.id.playerView);
        btnClosePlayerWrapper = view.findViewById(R.id.btnClosePlayerWrapper);
        btnClosePlayer = view.findViewById(R.id.btnClosePlayer);
        layoutPlayerError = view.findViewById(R.id.layoutPlayerError);
        btnPlayerRetry = view.findViewById(R.id.btnPlayerRetry);
        btnBack = view.findViewById(R.id.btnBack);
        btnBackWrapper = view.findViewById(R.id.btnBackWrapper);
    }

    private void setupRecyclers() {
        participantAdapter = new ParticipantDetailAdapter();
        recyclerParticipants.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerParticipants.setAdapter(participantAdapter);

        recordingAdapter = new RecordingAdapter(this::onRecordingClicked);
        recyclerRecordings.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerRecordings.setAdapter(recordingAdapter);
    }

    private void setupListeners() {
        View.OnClickListener back = v -> {
            if (playerOverlay.getVisibility() == View.VISIBLE) {
                closePlayer();
            } else {
                Navigation.findNavController(v).popBackStack();
            }
        };
        btnBack.setOnClickListener(back);
        btnBackWrapper.setOnClickListener(back);

        btnRetry.setOnClickListener(v -> viewModel.load());

        btnExpandParticipants.setOnClickListener(v -> {
            participantsExpanded = true;
            if (currentDetail != null) {
                bindParticipants(currentDetail.participants());
            }
        });

        btnClosePlayer.setOnClickListener(v -> closePlayer());
        btnClosePlayerWrapper.setOnClickListener(v -> closePlayer());

        btnPlayerRetry.setOnClickListener(v -> {
            if (currentRecordingUrl != null) {
                layoutPlayerError.setVisibility(View.GONE);
                playRecordingUrl(currentRecordingUrl);
            }
        });
    }

    private void restoreState(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) return;
        savedPlayerPosition = savedInstanceState.getLong(STATE_PLAYER_POSITION, 0L);
        savedPlayerWindow = savedInstanceState.getInt(STATE_PLAYER_WINDOW, 0);
        savedPlayWhenReady = savedInstanceState.getBoolean(STATE_PLAYER_PLAY_WHEN_READY, true);
        currentRecordingUrl = savedInstanceState.getString(STATE_CURRENT_RECORDING_URL);
        participantsExpanded = savedInstanceState.getBoolean(STATE_PARTICIPANTS_EXPANDED, false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (player != null) {
            outState.putLong(STATE_PLAYER_POSITION, player.getCurrentPosition());
            outState.putInt(STATE_PLAYER_WINDOW, player.getCurrentMediaItemIndex());
            outState.putBoolean(STATE_PLAYER_PLAY_WHEN_READY, player.getPlayWhenReady());
        } else {
            outState.putLong(STATE_PLAYER_POSITION, savedPlayerPosition);
            outState.putInt(STATE_PLAYER_WINDOW, savedPlayerWindow);
            outState.putBoolean(STATE_PLAYER_PLAY_WHEN_READY, savedPlayWhenReady);
        }
        outState.putString(STATE_CURRENT_RECORDING_URL, currentRecordingUrl);
        outState.putBoolean(STATE_PARTICIPANTS_EXPANDED, participantsExpanded);
    }

    private void observeViewModel() {
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(MeetingDetailUiState state) {
        switch (state) {
            case MeetingDetailUiState.Loading loading -> showLoading();
            case MeetingDetailUiState.Success success -> showSuccess(success.detail());
            case MeetingDetailUiState.Error error -> showError(error.message());
        }
    }

    private void showLoading() {
        progressLoading.setVisibility(View.VISIBLE);
        layoutErrorState.setVisibility(View.GONE);
        scrollContent.setVisibility(View.GONE);
    }

    private void showError(String message) {
        progressLoading.setVisibility(View.GONE);
        scrollContent.setVisibility(View.GONE);
        layoutErrorState.setVisibility(View.VISIBLE);
        tvErrorMessage.setText(
                message != null && !message.isEmpty()
                        ? message
                        : getString(R.string.meeting_history_error_subtitle));
    }

    private void showSuccess(MeetingHistoryDetail detail) {
        this.currentDetail = detail;
        progressLoading.setVisibility(View.GONE);
        layoutErrorState.setVisibility(View.GONE);
        scrollContent.setVisibility(View.VISIBLE);

        bindHeader(detail);
        bindTime(detail);
        bindDescription(detail.description());
        bindParticipants(detail.participants());
        bindRecordings(detail.recordings());
    }

    private void bindHeader(MeetingHistoryDetail detail) {
        String title = detail.title() != null && !detail.title().isEmpty()
                ? detail.title()
                : getString(R.string.meeting_history_untitled);
        tvTitle.setText(title);

        MeetingType type = detail.type();
        if (type != null) {
            tvTypeBadge.setVisibility(View.VISIBLE);
            tvTypeBadge.setText(localizedTypeBadge(type));
        } else {
            tvTypeBadge.setVisibility(View.GONE);
        }

        MeetingStatus status = detail.status();
        if (status == MeetingStatus.CANCELLED || status == MeetingStatus.ENDED) {
            tvStatusBadge.setVisibility(View.VISIBLE);
            tvStatusBadge.setText(localizedStatusBadge(status));
        } else if (status != null) {
            tvStatusBadge.setVisibility(View.VISIBLE);
            tvStatusBadge.setText(localizedStatusBadge(status));
        } else {
            tvStatusBadge.setVisibility(View.GONE);
        }
    }

    private String localizedTypeBadge(MeetingType type) {
        return switch (type) {
            case SCHEDULED -> getString(R.string.meeting_type_scheduled);
            case INSTANT -> getString(R.string.meeting_type_instant);
        };
    }

    private String localizedStatusBadge(MeetingStatus status) {
        return switch (status) {
            case SCHEDULED -> getString(R.string.meeting_status_scheduled);
            case LIVE -> getString(R.string.meeting_status_live);
            case ENDED -> getString(R.string.meeting_status_ended);
            case CANCELLED -> getString(R.string.meeting_status_cancelled);
        };
    }

    private void bindTime(MeetingHistoryDetail detail) {
        OffsetDateTime start = detail.startTime();
        OffsetDateTime end = detail.endTime();

        if (start == null) {
            tvDate.setText("");
            tvTimeRange.setText("");
            return;
        }

        DateTimeFormatter dateFmt =
                DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault());
        DateTimeFormatter timeFmt =
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                        .withLocale(Locale.getDefault());

        tvDate.setText(start.format(dateFmt));

        String startTime = start.format(timeFmt);
        if (end != null) {
            String endTime = end.format(timeFmt);
            String duration = formatDuration(start, end);
            tvTimeRange.setText(
                    getString(
                            R.string.meeting_detail_time_range_with_duration,
                            startTime,
                            endTime,
                            duration));
        } else {
            tvTimeRange.setText(
                    getString(R.string.meeting_detail_time_range_no_end, startTime));
        }
    }

    private String formatDuration(OffsetDateTime start, OffsetDateTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes <= 0) return "—";
        if (minutes < 60) {
            return getString(R.string.meeting_history_duration_minutes, minutes);
        }
        long hours = minutes / 60;
        long remaining = minutes % 60;
        if (remaining == 0) {
            return getString(R.string.meeting_history_duration_hours, hours);
        }
        return getString(R.string.meeting_history_duration_hours_minutes, hours, remaining);
    }

    private void bindDescription(@Nullable String description) {
        if (description == null || description.isBlank()) {
            sectionDescription.setVisibility(View.GONE);
        } else {
            sectionDescription.setVisibility(View.VISIBLE);
            tvDescription.setText(description);
        }
    }

    private void bindParticipants(List<MeetingParticipant> participants) {
        int total = participants != null ? participants.size() : 0;
        tvParticipantsHeader.setText(
                getString(R.string.meeting_detail_section_participants, total));

        if (total == 0) {
            participantAdapter.submitList(new ArrayList<>());
            btnExpandParticipants.setVisibility(View.GONE);
            return;
        }

        if (total > PARTICIPANTS_PREVIEW_LIMIT && !participantsExpanded) {
            participantAdapter.submitList(
                    new ArrayList<>(participants.subList(0, PARTICIPANTS_PREVIEW_LIMIT)));
            int remaining = total - PARTICIPANTS_PREVIEW_LIMIT;
            btnExpandParticipants.setVisibility(View.VISIBLE);
            btnExpandParticipants.setText(
                    getString(R.string.meeting_detail_participants_show_more, remaining));
        } else {
            participantAdapter.submitList(new ArrayList<>(participants));
            btnExpandParticipants.setVisibility(View.GONE);
        }
    }

    private void bindRecordings(List<MeetingRecording> recordings) {
        int count = recordings != null ? recordings.size() : 0;
        if (count == 0) {
            sectionRecordings.setVisibility(View.GONE);
            recordingAdapter.submitList(new ArrayList<>());
            return;
        }
        sectionRecordings.setVisibility(View.VISIBLE);
        tvRecordingsHeader.setText(getString(R.string.meeting_detail_section_recordings, count));
        recordingAdapter.submitList(new ArrayList<>(recordings));
    }

    private void onRecordingClicked(@NonNull MeetingRecording recording) {
        String url = recording.fileUrl();
        if (url == null || url.isBlank()) {
            showPlayerError();
            playerOverlay.setVisibility(View.VISIBLE);
            return;
        }
        currentRecordingUrl = url;
        savedPlayerPosition = 0L;
        savedPlayerWindow = 0;
        savedPlayWhenReady = true;
        playerOverlay.setVisibility(View.VISIBLE);
        layoutPlayerError.setVisibility(View.GONE);
        initPlayerIfNeeded();
        playRecordingUrl(url);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initPlayerIfNeeded() {
        if (player != null) return;
        player = new ExoPlayer.Builder(requireContext()).build();
        playerView.setPlayer(player);
        player.addListener(
                new Player.Listener() {
                    @Override
                    public void onPlayerError(@NonNull PlaybackException error) {
                        showPlayerError();
                    }
                });
    }

    private void playRecordingUrl(@NonNull String url) {
        if (player == null) {
            initPlayerIfNeeded();
        }
        if (player == null) return;
        MediaItem mediaItem = MediaItem.fromUri(url);
        player.setMediaItem(mediaItem, savedPlayerPosition);
        player.setPlayWhenReady(savedPlayWhenReady);
        player.prepare();
    }

    private void showPlayerError() {
        layoutPlayerError.setVisibility(View.VISIBLE);
    }

    private void closePlayer() {
        playerOverlay.setVisibility(View.GONE);
        layoutPlayerError.setVisibility(View.GONE);
        if (player != null) {
            player.stop();
            player.clearMediaItems();
        }
        savedPlayerPosition = 0L;
        savedPlayerWindow = 0;
        currentRecordingUrl = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (player != null && currentRecordingUrl != null
                && playerOverlay.getVisibility() == View.VISIBLE) {
            player.setPlayWhenReady(savedPlayWhenReady);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (player != null) {
            savedPlayerPosition = player.getCurrentPosition();
            savedPlayerWindow = player.getCurrentMediaItemIndex();
            savedPlayWhenReady = player.getPlayWhenReady();
            player.setPlayWhenReady(false);
        }
    }

    @Override
    public void onDestroyView() {
        if (player != null) {
            player.release();
            player = null;
        }
        if (playerView != null) {
            playerView.setPlayer(null);
        }
        super.onDestroyView();
    }
}
