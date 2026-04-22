package io.github.phunguy65.zms.presentation.videocall;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import io.github.phunguy65.zms.domain.model.JoinRequestItem;
import io.github.phunguy65.zms.frontends.R;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying pending join requests
 * in the waiting room bottom sheet with approve/deny actions per item.
 */
public class JoinRequestAdapter extends RecyclerView.Adapter<JoinRequestAdapter.ViewHolder> {

    private final List<JoinRequestItem> items = new ArrayList<>();
    private final ActionCallback actionCallback;

    public JoinRequestAdapter(ActionCallback actionCallback) {
        this.actionCallback = actionCallback;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_join_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JoinRequestItem item = items.get(position);
        holder.tvDisplayName.setText(item.getDisplayName());

        if (item.getRequestedAt() != null && !item.getRequestedAt().isEmpty()) {
            holder.tvRequestedAt.setVisibility(View.VISIBLE);
            holder.tvRequestedAt.setText(item.getRequestedAt());
        } else {
            holder.tvRequestedAt.setVisibility(View.GONE);
        }

        holder.btnApprove.setOnClickListener(v -> {
            if (actionCallback != null) {
                actionCallback.onApprove(item);
            }
        });

        holder.btnDeny.setOnClickListener(v -> {
            if (actionCallback != null) {
                actionCallback.onDeny(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitList(List<JoinRequestItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    /**
     * Callback interface for approve/deny actions on a join request.
     */
    public interface ActionCallback {
        void onApprove(JoinRequestItem item);

        void onDeny(JoinRequestItem item);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDisplayName, tvRequestedAt;
        MaterialButton btnApprove, btnDeny;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDisplayName = itemView.findViewById(R.id.tvDisplayName);
            tvRequestedAt = itemView.findViewById(R.id.tvRequestedAt);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnDeny = itemView.findViewById(R.id.btnDeny);
        }
    }
}
