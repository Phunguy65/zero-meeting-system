package io.github.phunguy65.zms.presentation.main.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import io.github.phunguy65.zms.domain.model.MeetingRecording;
import io.github.phunguy65.zms.frontends.R;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Objects;

/**
 * RecyclerView adapter for recordings on the meeting detail screen.
 *
 * <p>Each item shows the recording label (index-based, e.g. "Recording 1"), the recording creation
 * date as subtitle, and a formatted duration (MM:SS or HH:MM:SS). Tapping an item delegates to
 * {@link OnItemClickListener#onRecordingClicked} to start in-app playback.
 */
public class RecordingAdapter
        extends ListAdapter<MeetingRecording, RecordingAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onRecordingClicked(@NonNull MeetingRecording recording);
    }

    private final OnItemClickListener listener;

    public RecordingAdapter(@NonNull OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recording, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), position + 1, listener);
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView card;
        private final TextView tvRecordingLabel;
        private final TextView tvRecordingSubtitle;
        private final TextView tvDuration;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvRecordingLabel = itemView.findViewById(R.id.tvRecordingLabel);
            tvRecordingSubtitle = itemView.findViewById(R.id.tvRecordingSubtitle);
            tvDuration = itemView.findViewById(R.id.tvDuration);
        }

        void bind(
                @NonNull MeetingRecording item,
                int indexOneBased,
                @NonNull OnItemClickListener listener) {
            tvRecordingLabel.setText(
                    card.getContext()
                            .getString(R.string.meeting_detail_recording_label, indexOneBased));

            if (item.createdAt() != null) {
                tvRecordingSubtitle.setVisibility(View.VISIBLE);
                tvRecordingSubtitle.setText(
                        item.createdAt()
                                .format(
                                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                                                .withLocale(Locale.getDefault())));
            } else {
                tvRecordingSubtitle.setVisibility(View.GONE);
            }

            tvDuration.setText(formatDuration(item.durationSeconds()));
            card.setOnClickListener(v -> listener.onRecordingClicked(item));
        }

        private String formatDuration(Integer seconds) {
            if (seconds == null || seconds <= 0) {
                return "—";
            }
            int s = seconds;
            int h = s / 3600;
            int m = (s % 3600) / 60;
            int sec = s % 60;
            if (h > 0) {
                return String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, sec);
            }
            return String.format(Locale.getDefault(), "%d:%02d", m, sec);
        }
    }

    private static final DiffUtil.ItemCallback<MeetingRecording> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull MeetingRecording oldItem, @NonNull MeetingRecording newItem) {
                    return Objects.equals(oldItem.id(), newItem.id());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull MeetingRecording oldItem, @NonNull MeetingRecording newItem) {
                    return Objects.equals(oldItem, newItem);
                }
            };
}
