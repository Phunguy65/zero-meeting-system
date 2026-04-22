package io.github.phunguy65.zms.presentation.videocall;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import io.github.phunguy65.zms.domain.model.VideoParticipant;
import io.github.phunguy65.zms.frontends.R;
import java.util.HashSet;
import java.util.Set;

/**
 * RecyclerView adapter for the video call participant grid.
 * Handles SurfaceViewRenderer attach/detach for video tracks.
 */
public class VideoGridAdapter extends ListAdapter<VideoParticipant, VideoTileViewHolder> {

    private final Set<String> activeSpeakerIds = new HashSet<>();

    private static final DiffUtil.ItemCallback<VideoParticipant> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<VideoParticipant>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull VideoParticipant oldItem, @NonNull VideoParticipant newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull VideoParticipant oldItem, @NonNull VideoParticipant newItem) {
                    return oldItem.getId().equals(newItem.getId())
                            && oldItem.isMicEnabled() == newItem.isMicEnabled()
                            && oldItem.isCameraEnabled() == newItem.isCameraEnabled()
                            && oldItem.isActiveSpeaker() == newItem.isActiveSpeaker()
                            && (oldItem.getVideoTrack() == newItem.getVideoTrack());
                }
            };

    public VideoGridAdapter() {
        super(DIFF_CALLBACK);
    }

    /**
     * Updates the active speaker IDs and notifies changes.
     */
    public void setActiveSpeakers(Set<String> speakerIds) {
        this.activeSpeakerIds.clear();
        this.activeSpeakerIds.addAll(speakerIds);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VideoTileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video_tile, parent, false);
        return new VideoTileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoTileViewHolder holder, int position) {
        VideoParticipant participant = getItem(position);
        boolean isActiveSpeaker =
                activeSpeakerIds.contains(participant.getId()) || participant.isActiveSpeaker();
        holder.bind(participant, isActiveSpeaker);
    }

    @Override
    public void onViewRecycled(@NonNull VideoTileViewHolder holder) {
        super.onViewRecycled(holder);
        holder.unbind();
    }
}
