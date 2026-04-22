package io.github.phunguy65.zms.presentation.videocall;

import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import io.github.phunguy65.zms.domain.model.VideoParticipant;
import io.github.phunguy65.zms.frontends.R;
import io.livekit.android.renderer.SurfaceViewRenderer;
import io.livekit.android.room.track.VideoTrack;
import livekit.org.webrtc.EglBase;

/**
 * ViewHolder for video tiles in the call grid.
 * Manages SurfaceViewRenderer lifecycle for video track rendering.
 */
public class VideoTileViewHolder extends RecyclerView.ViewHolder {

    private final MaterialCardView cardVideoTile;
    private final FrameLayout videoRendererContainer;
    private final LinearLayout cameraOffPlaceholder;
    private final TextView tvCameraOffName;
    private final View activeSpeakerBorder;
    private final TextView tvParticipantName;
    private final ImageView imgMutedBadge;

    private SurfaceViewRenderer surfaceViewRenderer;
    private VideoTrack currentVideoTrack;

    public VideoTileViewHolder(@NonNull View itemView) {
        super(itemView);
        cardVideoTile = itemView.findViewById(R.id.cardVideoTile);
        videoRendererContainer = itemView.findViewById(R.id.videoRendererContainer);
        cameraOffPlaceholder = itemView.findViewById(R.id.cameraOffPlaceholder);
        tvCameraOffName = itemView.findViewById(R.id.tvCameraOffName);
        activeSpeakerBorder = itemView.findViewById(R.id.activeSpeakerBorder);
        tvParticipantName = itemView.findViewById(R.id.tvParticipantName);
        imgMutedBadge = itemView.findViewById(R.id.imgMutedBadge);
    }

    /**
     * Binds participant data to the tile.
     */
    public void bind(VideoParticipant participant, boolean isActiveSpeaker) {
        // Set participant name
        tvParticipantName.setText(participant.getDisplayName());
        tvCameraOffName.setText(participant.getDisplayName());

        // Show/hide muted badge
        imgMutedBadge.setVisibility(participant.isMicEnabled() ? View.GONE : View.VISIBLE);

        // Show/hide active speaker border
        activeSpeakerBorder.setVisibility(isActiveSpeaker ? View.VISIBLE : View.GONE);

        // Handle video track
        VideoTrack videoTrack = participant.getVideoTrack();
        boolean hasVideo = videoTrack != null && participant.isCameraEnabled();

        if (hasVideo) {
            // Show video
            cameraOffPlaceholder.setVisibility(View.GONE);
            attachVideoTrack(videoTrack);
        } else {
            // Show camera off placeholder
            cameraOffPlaceholder.setVisibility(View.VISIBLE);
            detachVideoTrack();
        }
    }

    /**
     * Attaches a video track to the surface renderer.
     */
    private void attachVideoTrack(VideoTrack videoTrack) {
        if (videoTrack == currentVideoTrack && surfaceViewRenderer != null) {
            // Already attached to this track
            return;
        }

        // Detach previous track if different
        detachVideoTrack();

        // Create surface renderer if needed
        if (surfaceViewRenderer == null) {
            surfaceViewRenderer = new SurfaceViewRenderer(itemView.getContext());
            surfaceViewRenderer.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

            // Initialize renderer with EGL context
            EglBase.Context eglContext = EglBase.create().getEglBaseContext();
            surfaceViewRenderer.init(eglContext, null);
            surfaceViewRenderer.setMirror(false);
            surfaceViewRenderer.setEnableHardwareScaler(true);
        }

        // Add to container if not already added
        if (surfaceViewRenderer.getParent() == null) {
            videoRendererContainer.addView(surfaceViewRenderer);
        }

        // Attach track to renderer
        videoTrack.addRenderer(surfaceViewRenderer);
        currentVideoTrack = videoTrack;
    }

    /**
     * Detaches the current video track from the surface renderer.
     */
    private void detachVideoTrack() {
        if (currentVideoTrack != null && surfaceViewRenderer != null) {
            currentVideoTrack.removeRenderer(surfaceViewRenderer);
        }
        currentVideoTrack = null;
    }

    /**
     * Cleans up resources when the view is recycled.
     */
    public void unbind() {
        detachVideoTrack();
        if (surfaceViewRenderer != null) {
            videoRendererContainer.removeView(surfaceViewRenderer);
            surfaceViewRenderer.release();
            surfaceViewRenderer = null;
        }
    }
}
