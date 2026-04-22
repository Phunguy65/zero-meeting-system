package io.github.phunguy65.zms.presentation.meeting.participant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.MaterialColors;
import io.github.phunguy65.zms.domain.model.Participant;
import io.github.phunguy65.zms.domain.model.ParticipantRole;
import io.github.phunguy65.zms.frontends.R;
import java.util.List;

/**
 * RecyclerView adapter for the participants bottom sheet.
 * Displays role badges (Host/Guest), local-participant marker, mic/camera state,
 * and binds against the refactored {@link Participant} model fields.
 */
public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ViewHolder> {

    private final List<Participant> participantList;

    public ParticipantAdapter(List<Participant> participantList) {
        this.participantList = participantList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_participant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Participant p = participantList.get(position);
        holder.tvName.setText(p.getName());

        bindLocalIndicator(holder, p);
        bindRoleBadge(holder, p);
        bindMicState(holder, p);
        bindCameraState(holder, p);
    }

    private void bindLocalIndicator(ViewHolder holder, Participant p) {
        if (p.isLocal()) {
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText(R.string.participant_me_suffix);
        } else {
            holder.tvStatus.setVisibility(View.GONE);
        }
    }

    private void bindRoleBadge(ViewHolder holder, Participant p) {
        ParticipantRole role = p.getRole();
        if (role == ParticipantRole.HOST) {
            holder.tvRoleBadge.setVisibility(View.VISIBLE);
            holder.tvRoleBadge.setText(R.string.participant_role_host);
        } else if (role == ParticipantRole.GUEST) {
            holder.tvRoleBadge.setVisibility(View.VISIBLE);
            holder.tvRoleBadge.setText(R.string.participant_role_guest);
        } else {
            holder.tvRoleBadge.setVisibility(View.GONE);
        }
    }

    private void bindMicState(ViewHolder holder, Participant p) {
        if (p.isMicOn()) {
            int colorOnSurface = MaterialColors.getColor(
                    holder.itemView, com.google.android.material.R.attr.colorOnSurface);
            holder.btnMic.setColorFilter(colorOnSurface);
            holder.btnMic.setBackgroundTintList(null);
        } else {
            int colorError =
                    MaterialColors.getColor(holder.itemView, androidx.appcompat.R.attr.colorError);
            int colorErrorContainer = MaterialColors.getColor(
                    holder.itemView, com.google.android.material.R.attr.colorErrorContainer);
            holder.btnMic.setColorFilter(colorError);
            holder.btnMic.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(colorErrorContainer));
        }
    }

    private void bindCameraState(ViewHolder holder, Participant p) {
        if (p.isVideoOn()) {
            int colorOnSurface = MaterialColors.getColor(
                    holder.itemView, com.google.android.material.R.attr.colorOnSurface);
            holder.btnCamera.setColorFilter(colorOnSurface);
            holder.btnCamera.setBackgroundTintList(null);
        } else {
            int colorOnSurfaceVariant = MaterialColors.getColor(
                    holder.itemView, com.google.android.material.R.attr.colorOnSurfaceVariant);
            int colorSurfaceVariant = MaterialColors.getColor(
                    holder.itemView, com.google.android.material.R.attr.colorSurfaceVariant);
            holder.btnCamera.setColorFilter(colorOnSurfaceVariant);
            holder.btnCamera.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(colorSurfaceVariant));
        }
    }

    @Override
    public int getItemCount() {
        return participantList.size();
    }

    public void updateList(List<Participant> newList) {
        participantList.clear();
        participantList.addAll(newList);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus, tvRoleBadge;
        ImageView btnMic, btnCamera;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvRoleBadge = itemView.findViewById(R.id.tvRoleBadge);
            btnMic = itemView.findViewById(R.id.btnMic);
            btnCamera = itemView.findViewById(R.id.btnCamera);
        }
    }
}
