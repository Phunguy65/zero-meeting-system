package io.github.phunguy65.zms.presentation.main.calendar;

import android.view.View;
import android.widget.TextView;
import com.kizitonwose.calendar.view.ViewContainer;
import io.github.phunguy65.zms.frontends.R;

/**
 * ViewContainer for calendar month header.
 *
 * <p>Holds references to the 7 weekday label TextViews (Mon-Sun).
 */
public class HeaderViewContainer extends ViewContainer {

    public final TextView[] weekdayLabels;

    public HeaderViewContainer(View view) {
        super(view);
        weekdayLabels = new TextView[] {
            view.findViewById(R.id.tvWeekday0),
            view.findViewById(R.id.tvWeekday1),
            view.findViewById(R.id.tvWeekday2),
            view.findViewById(R.id.tvWeekday3),
            view.findViewById(R.id.tvWeekday4),
            view.findViewById(R.id.tvWeekday5),
            view.findViewById(R.id.tvWeekday6)
        };
    }
}
