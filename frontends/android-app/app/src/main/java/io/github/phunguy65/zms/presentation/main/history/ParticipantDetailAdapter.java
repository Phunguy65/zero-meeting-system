package io.github.phunguy65.zms.presentation.main.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.github.phunguy65.zms.domain.model.MeetingParticipant;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.common.util.InitialsDrawable;
import java.util.Objects;

/**
 * RecyclerView adapter for participants shown on the meeting detail screen.
 *
 * <p>Uses {@link InitialsDrawable} as an avatar fallback — the meeting detail response does not
 * currently include avatar URLs, so every row shows deterministic initials keyed by {@code userId}.
 */
public class ParticipantDetailAdapter
        extends ListAdapter<MeetingParticipant, ParticipantDetailAdapter.ViewHolder> {

    public ParticipantDetailAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_participant_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imgAvatar;
        private final TextView tvDisplayName;
        private final TextView tvRoleBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvDisplayName = itemView.findViewById(R.id.tvDisplayName);
            tvRoleBadge = itemView.findViewById(R.id.tvRoleBadge);
        }

        void bind(@NonNull MeetingParticipant item) {
            String name = item.displayName() != null ? item.displayName() : "";
            tvDisplayName.setText(name);
            imgAvatar.setImageDrawable(new InitialsDrawable(name, item.userId()));

            String role = item.role();
            if (role == null || role.isBlank()) {
                tvRoleBadge.setVisibility(View.GONE);
            } else {
                tvRoleBadge.setVisibility(View.VISIBLE);
                tvRoleBadge.setText(role);
            }
        }
    }

    private static final DiffUtil.ItemCallback<MeetingParticipant> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull MeetingParticipant oldItem, @NonNull MeetingParticipant newItem) {
                    return Objects.equals(oldItem.userId(), newItem.userId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull MeetingParticipant oldItem, @NonNull MeetingParticipant newItem) {
                    return Objects.equals(oldItem, newItem);
                }
            };
}
