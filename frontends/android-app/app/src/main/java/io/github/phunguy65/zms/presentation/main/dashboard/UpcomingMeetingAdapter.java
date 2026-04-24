package io.github.phunguy65.zms.presentation.main.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import io.github.phunguy65.zms.domain.model.MeetingStatus;
import io.github.phunguy65.zms.domain.model.UpcomingMeeting;
import io.github.phunguy65.zms.frontends.R;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Objects;

/**
 * RecyclerView adapter for upcoming meeting list items with DiffUtil-powered updates.
 *
 * <p>Displays meeting cards with title, time range, join action, and more options menu.
 * The join button is enabled for LIVE meetings, disabled (showing "Wait") for SCHEDULED meetings.
 */
public class UpcomingMeetingAdapter
        extends ListAdapter<UpcomingMeeting, UpcomingMeetingAdapter.ViewHolder> {

    public interface OnJoinClickListener {
        void onJoinClicked(@NonNull UpcomingMeeting meeting);
    }

    public interface OnMoreOptionsClickListener {
        void onMoreOptionsClicked(@NonNull View anchor, @NonNull UpcomingMeeting meeting);
    }

    private final OnJoinClickListener joinListener;
    private final OnMoreOptionsClickListener moreOptionsListener;

    public UpcomingMeetingAdapter(
            @NonNull OnJoinClickListener joinListener,
            @NonNull OnMoreOptionsClickListener moreOptionsListener) {
        super(DIFF_CALLBACK);
        this.joinListener = joinListener;
        this.moreOptionsListener = moreOptionsListener;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upcoming_meeting, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), joinListener, moreOptionsListener);
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvTimeRange;
        private final MaterialButton btnJoin;
        private final View btnMoreOptions;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTimeRange = itemView.findViewById(R.id.tvTimeRange);
            btnJoin = itemView.findViewById(R.id.btnJoin);
            btnMoreOptions = itemView.findViewById(R.id.btnMoreOptions);
        }

        void bind(
                @NonNull UpcomingMeeting item,
                @NonNull OnJoinClickListener joinListener,
                @NonNull OnMoreOptionsClickListener moreOptionsListener) {
            String title = item.title();
            tvTitle.setText(
                    title != null && !title.isEmpty()
                            ? title
                            : itemView.getContext().getString(R.string.meeting_history_untitled));

            tvTimeRange.setText(formatTimeRange(item.startTime(), item.endTime()));

            boolean isLive = item.status() == MeetingStatus.LIVE;
            if (isLive) {
                btnJoin.setText(R.string.action_join);
                int colorPrimary =
                        MaterialColors.getColor(itemView, androidx.appcompat.R.attr.colorPrimary);
                btnJoin.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(colorPrimary));
                btnJoin.setEnabled(true);
            } else {
                btnJoin.setText(R.string.action_wait);
                btnJoin.setEnabled(true);
            }

            btnJoin.setOnClickListener(v -> joinListener.onJoinClicked(item));
            btnMoreOptions.setOnClickListener(
                    v -> moreOptionsListener.onMoreOptionsClicked(v, item));
        }

        private String formatTimeRange(OffsetDateTime startTime, OffsetDateTime endTime) {
            if (startTime == null) {
                return "";
            }

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                    .withLocale(Locale.getDefault());

            String start = startTime.format(timeFormatter);

            if (endTime != null) {
                String end = endTime.format(timeFormatter);
                return start + " - " + end;
            }

            return start;
        }
    }

    private static final DiffUtil.ItemCallback<UpcomingMeeting> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull UpcomingMeeting oldItem, @NonNull UpcomingMeeting newItem) {
                    return Objects.equals(oldItem.id(), newItem.id());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull UpcomingMeeting oldItem, @NonNull UpcomingMeeting newItem) {
                    return Objects.equals(oldItem, newItem);
                }
            };
}
