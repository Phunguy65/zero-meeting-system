package io.github.phunguy65.zms.presentation.main.history;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import io.github.phunguy65.zms.domain.model.MeetingHistory;
import io.github.phunguy65.zms.domain.model.MeetingStatus;
import io.github.phunguy65.zms.domain.model.MeetingType;
import io.github.phunguy65.zms.frontends.R;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Objects;

/**
 * RecyclerView adapter for meeting history list items with DiffUtil-powered updates.
 *
 * <p>Applies cancelled meeting visual treatment per design D5: strikethrough title, 0.7 alpha,
 * and red CANCELLED badge.
 */
public class MeetingHistoryAdapter
        extends ListAdapter<MeetingHistory, MeetingHistoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onMeetingClicked(@NonNull MeetingHistory meeting);
    }

    private final OnItemClickListener listener;

    public MeetingHistoryAdapter(@NonNull OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meeting_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView card;
        private final TextView tvTitle;
        private final TextView tvDateTime;
        private final TextView tvDuration;
        private final TextView tvTypeBadge;
        private final TextView tvCancelledBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvTypeBadge = itemView.findViewById(R.id.tvTypeBadge);
            tvCancelledBadge = itemView.findViewById(R.id.tvCancelledBadge);
        }

        void bind(@NonNull MeetingHistory item, @NonNull OnItemClickListener listener) {
            String title = item.title() != null && !item.title().isEmpty()
                    ? item.title()
                    : card.getContext().getString(R.string.meeting_history_untitled);
            tvTitle.setText(title);

            tvDateTime.setText(formatStartDateTime(item.startTime()));
            tvDuration.setText(formatDuration(item.startTime(), item.endTime()));

            tvTypeBadge.setText(badgeTextForType(item.type()));

            boolean cancelled = item.status() == MeetingStatus.CANCELLED;
            if (cancelled) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                card.setAlpha(0.7f);
                tvCancelledBadge.setVisibility(View.VISIBLE);
                String dateTime = formatStartDateTime(item.startTime());
                card.setContentDescription(card.getContext()
                        .getString(R.string.cd_meeting_cancelled_format, title, dateTime));
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                card.setAlpha(1.0f);
                tvCancelledBadge.setVisibility(View.GONE);
                card.setContentDescription(null);
            }

            card.setOnClickListener(v -> listener.onMeetingClicked(item));
        }

        private String badgeTextForType(MeetingType type) {
            if (type == null) {
                return "";
            }
            return switch (type) {
                case SCHEDULED -> card.getContext().getString(R.string.meeting_type_scheduled);
                case INSTANT -> card.getContext().getString(R.string.meeting_type_instant);
            };
        }

        private String formatStartDateTime(OffsetDateTime startTime) {
            if (startTime == null) {
                return "";
            }
            DateTimeFormatter date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault());
            DateTimeFormatter time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                    .withLocale(Locale.getDefault());
            return startTime.format(date) + " · " + startTime.format(time);
        }

        private String formatDuration(OffsetDateTime start, OffsetDateTime end) {
            if (start == null || end == null) {
                return "—";
            }
            long minutes = Duration.between(start, end).toMinutes();
            if (minutes <= 0) {
                return "—";
            }
            if (minutes < 60) {
                return card.getContext()
                        .getString(R.string.meeting_history_duration_minutes, minutes);
            }
            long hours = minutes / 60;
            long remaining = minutes % 60;
            if (remaining == 0) {
                return card.getContext().getString(R.string.meeting_history_duration_hours, hours);
            }
            return card.getContext()
                    .getString(R.string.meeting_history_duration_hours_minutes, hours, remaining);
        }
    }

    private static final DiffUtil.ItemCallback<MeetingHistory> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull MeetingHistory oldItem, @NonNull MeetingHistory newItem) {
                    return Objects.equals(oldItem.id(), newItem.id());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull MeetingHistory oldItem, @NonNull MeetingHistory newItem) {
                    return Objects.equals(oldItem, newItem);
                }
            };
}
