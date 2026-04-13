package io.github.phunguy65.zms.presentation.meeting.participant;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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

        // Xử lý Trạng thái Role (Host, Me)
        if (p.getRoleStatus() != null && !p.getRoleStatus().isEmpty()) {
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText(p.getRoleStatus());
            holder.tvName.setPadding(0, 0, 0, 0); // Reset padding
        } else {
            holder.tvStatus.setVisibility(View.GONE);
        }

        // Xử lý Trạng thái Connection (Connecting...)
        if (p.getConnectionStatus() != null && !p.getConnectionStatus().isEmpty()) {
            holder.tvSubStatus.setVisibility(View.VISIBLE);
            holder.tvSubStatus.setText(p.getConnectionStatus());
        } else {
            holder.tvSubStatus.setVisibility(View.GONE);
        }

        // Dấu chấm đỏ cảnh báo
        holder.indicatorRed.setVisibility(p.isHasAlert() ? View.VISIBLE : View.GONE);

        // Xử lý Màu sắc Mic
        if (p.isMicOn()) {
            holder.btnMic.setColorFilter(Color.parseColor("#333333")); // Xám đậm
            holder.btnMic.setBackgroundTintList(null); // Nền xám nhạt mặc định
        } else {
            holder.btnMic.setColorFilter(Color.parseColor("#E53E3E")); // Màu đỏ
            holder.btnMic.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#FFF0F0"))); // Nền hồng nhạt
        }

        // Xử lý Màu sắc Video
        if (p.isVideoOn()) {
            holder.btnCamera.setColorFilter(Color.parseColor("#333333"));
            holder.btnCamera.setBackgroundTintList(null);
        } else {
            // Giả lập màu xám cho video tắt (giống trong thiết kế)
            holder.btnCamera.setColorFilter(Color.parseColor("#999999"));
            holder.btnCamera.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#F5F5F5")));
        }
    }

    @Override
    public int getItemCount() {
        return participantList.size();
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
