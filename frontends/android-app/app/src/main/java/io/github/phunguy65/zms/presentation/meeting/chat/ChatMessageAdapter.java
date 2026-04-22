package io.github.phunguy65.zms.presentation.meeting.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.github.phunguy65.zms.domain.model.ChatMessage;
import io.github.phunguy65.zms.frontends.R;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * RecyclerView adapter for the meeting chat timeline. Renders three distinct
 * view types: outgoing messages (right-aligned), incoming messages
 * (left-aligned), and system messages (centered gray text).
 */
public class ChatMessageAdapter extends ListAdapter<ChatMessage, RecyclerView.ViewHolder> {

    private static final int TYPE_OUTGOING = 0;
    private static final int TYPE_INCOMING = 1;
    private static final int TYPE_SYSTEM = 2;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private String currentUserId;

    public ChatMessageAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = getItem(position);
        if (message.isSystem()) {
            return TYPE_SYSTEM;
        }
        return message.isMine(currentUserId) ? TYPE_OUTGOING : TYPE_INCOMING;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return switch (viewType) {
            case TYPE_OUTGOING ->
                new OutgoingViewHolder(
                        inflater.inflate(R.layout.item_chat_outgoing, parent, false));
            case TYPE_SYSTEM ->
                new SystemViewHolder(inflater.inflate(R.layout.item_chat_system, parent, false));
            default ->
                new IncomingViewHolder(
                        inflater.inflate(R.layout.item_chat_incoming, parent, false));
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = getItem(position);
        switch (holder) {
            case OutgoingViewHolder h -> h.bind(message);
            case IncomingViewHolder h -> h.bind(message);
            case SystemViewHolder h -> h.bind(message);
            default -> {}
        }
    }

    static class OutgoingViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSenderName;
        private final TextView tvContent;
        private final TextView tvTimestamp;

        OutgoingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }

        void bind(ChatMessage message) {
            tvSenderName.setVisibility(View.GONE);
            tvContent.setText(message.getContent());
            tvTimestamp.setText(formatTime(message.getCreatedAt()));
        }
    }

    static class IncomingViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSenderName;
        private final TextView tvContent;
        private final TextView tvTimestamp;

        IncomingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }

        void bind(ChatMessage message) {
            String sender = message.getSenderName();
            if (sender != null && !sender.isEmpty()) {
                tvSenderName.setText(sender);
                tvSenderName.setVisibility(View.VISIBLE);
            } else {
                tvSenderName.setVisibility(View.GONE);
            }
            tvContent.setText(message.getContent());
            tvTimestamp.setText(formatTime(message.getCreatedAt()));
        }
    }

    static class SystemViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSystemMessage;

        SystemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSystemMessage = itemView.findViewById(R.id.tvSystemMessage);
        }

        void bind(ChatMessage message) {
            tvSystemMessage.setText(message.getContent());
        }
    }

    private static String formatTime(OffsetDateTime dateTime) {
        if (dateTime == null) return "";
        try {
            return dateTime.format(TIME_FORMAT);
        } catch (Exception e) {
            return "";
        }
    }

    private static final DiffUtil.ItemCallback<ChatMessage> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                    return oldItem.getId() != null && oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
                    return oldItem.equals(newItem) && oldItem.getSeqNum() == newItem.getSeqNum();
                }
            };
}
