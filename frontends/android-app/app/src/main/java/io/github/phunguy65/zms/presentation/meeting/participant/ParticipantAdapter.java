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
import io.github.phunguy65.zms.frontends.R;
import java.util.List;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ViewHolder> {

    private final List<Participant> participantList;

    public ParticipantAdapter(List<Participant> participantList) {
        this.participantList = participantList;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_participant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Participant p = participantList.get(position);
        holder.tvName.setText(p.getName());

        // Role status (Host, Me)
        if (p.getRoleStatus() != null && !p.getRoleStatus().isEmpty()) {
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText(p.getRoleStatus());
            holder.tvName.setPadding(0, 0, 0, 0);
        } else {
            holder.tvStatus.setVisibility(View.GONE);
        }

        // Connection status (Connecting...)
        if (p.getConnectionStatus() != null && !p.getConnectionStatus().isEmpty()) {
            holder.tvSubStatus.setVisibility(View.VISIBLE);
            holder.tvSubStatus.setText(p.getConnectionStatus());
        } else {
            holder.tvSubStatus.setVisibility(View.GONE);
        }

        // Red alert indicator
        holder.indicatorRed.setVisibility(p.isHasAlert() ? View.VISIBLE : View.GONE);

        // Mic icon color using theme colors
        if (p.isMicOn()) {
            int colorOnSurface = MaterialColors.getColor(holder.itemView, 
                    com.google.android.material.R.attr.colorOnSurface);
            holder.btnMic.setColorFilter(colorOnSurface);
            holder.btnMic.setBackgroundTintList(null);
        } else {
            int colorError = MaterialColors.getColor(holder.itemView, 
                    androidx.appcompat.R.attr.colorError);
            int colorErrorContainer = MaterialColors.getColor(holder.itemView, 
                    com.google.android.material.R.attr.colorErrorContainer);
            holder.btnMic.setColorFilter(colorError);
            holder.btnMic.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(colorErrorContainer));
        }

        // Camera icon color using theme colors
        if (p.isVideoOn()) {
            int colorOnSurface = MaterialColors.getColor(holder.itemView, 
                    com.google.android.material.R.attr.colorOnSurface);
            holder.btnCamera.setColorFilter(colorOnSurface);
            holder.btnCamera.setBackgroundTintList(null);
        } else {
            int colorOnSurfaceVariant = MaterialColors.getColor(holder.itemView, 
                    com.google.android.material.R.attr.colorOnSurfaceVariant);
            int colorSurfaceVariant = MaterialColors.getColor(holder.itemView, 
                    com.google.android.material.R.attr.colorSurfaceVariant);
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
        TextView tvName, tvStatus, tvSubStatus;
        ImageView btnMic, btnCamera;
        View indicatorRed;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvSubStatus = itemView.findViewById(R.id.tvSubStatus);
            btnMic = itemView.findViewById(R.id.btnMic);
            btnCamera = itemView.findViewById(R.id.btnCamera);
            indicatorRed = itemView.findViewById(R.id.indicatorRed);
        }
    }
}
