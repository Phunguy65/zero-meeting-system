package io.github.phunguy65.zms.presentation.main.calendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.github.phunguy65.zms.domain.model.CalendarEvent;
import io.github.phunguy65.zms.domain.model.MeetingStatus;
import io.github.phunguy65.zms.frontends.R;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * RecyclerView adapter for calendar event list items with DiffUtil-powered updates.
 *
 * <p>Displays event cards with time, title, and status indicator.
 */
public class CalendarEventAdapter
        extends ListAdapter<CalendarEvent, CalendarEventAdapter.ViewHolder> {

    public CalendarEventAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvTime;
        private final TextView tvAmPm;
        private final View viewStatusIndicator;
        private final TextView tvTitle;
        private final LinearLayout statusContainer;
        private final View viewStatusDot;
        private final TextView tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvAmPm = itemView.findViewById(R.id.tvAmPm);
            viewStatusIndicator = itemView.findViewById(R.id.viewStatusIndicator);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            statusContainer = itemView.findViewById(R.id.statusContainer);
            viewStatusDot = itemView.findViewById(R.id.viewStatusDot);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }

        void bind(@NonNull CalendarEvent item) {
            // Format time
            OffsetDateTime startTime = item.startTime();
            if (startTime != null) {
                DateTimeFormatter timeFormatter =
                        DateTimeFormatter.ofPattern("h:mm", Locale.getDefault());
                DateTimeFormatter amPmFormatter =
                        DateTimeFormatter.ofPattern("a", Locale.getDefault());
                tvTime.setText(startTime.format(timeFormatter));
                tvAmPm.setText(startTime.format(amPmFormatter));
            } else {
                tvTime.setText("");
                tvAmPm.setText("");
            }

            // Set title
            String title = item.title();
            tvTitle.setText(
                    title != null && !title.isEmpty()
                            ? title
                            : itemView.getContext().getString(R.string.meeting_history_untitled));

            // Set status indicator
            boolean isLive = item.status() == MeetingStatus.LIVE;
            if (isLive) {
                viewStatusIndicator.setBackgroundColor(itemView.getContext()
                        .getColor(com.google
                                .android
                                .material
                                .R
                                .color
                                .m3_ref_palette_dynamic_primary40));
                tvTime.setTextColor(itemView.getContext()
                        .getColor(com.google.android.material.R.color.m3_ref_palette_black));
                tvTime.setTextAppearance(
                        com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);

                statusContainer.setVisibility(View.VISIBLE);
                tvStatus.setText(R.string.meeting_status_in_progress);
            } else {
                viewStatusIndicator.setBackgroundColor(itemView.getContext()
                        .getColor(com.google.android.material.R.color.m3_ref_palette_neutral60));
                tvTime.setTextColor(itemView.getContext()
                        .getColor(com.google.android.material.R.color.m3_ref_palette_neutral40));
                tvTime.setTextAppearance(
                        com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);

                statusContainer.setVisibility(View.GONE);
            }
        }
    }

    private static final DiffUtil.ItemCallback<CalendarEvent> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull CalendarEvent oldItem, @NonNull CalendarEvent newItem) {
                    return Objects.equals(oldItem.id(), newItem.id());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull CalendarEvent oldItem, @NonNull CalendarEvent newItem) {
                    return Objects.equals(oldItem, newItem);
                }
            };
}
