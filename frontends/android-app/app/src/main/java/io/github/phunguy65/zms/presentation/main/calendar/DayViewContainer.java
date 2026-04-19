package io.github.phunguy65.zms.presentation.main.calendar;

import android.view.View;
import android.widget.TextView;
import com.kizitonwose.calendar.view.ViewContainer;
import io.github.phunguy65.zms.frontends.R;

/**
 * ViewContainer for calendar day cells.
 *
 * <p>Holds references to the day number text and event indicator dot view.
 */
public class DayViewContainer extends ViewContainer {

    public final TextView tvDayNumber;
    public final View viewEventDot;

    public DayViewContainer(View view) {
        super(view);
        tvDayNumber = view.findViewById(R.id.tvDayNumber);
        viewEventDot = view.findViewById(R.id.viewEventDot);
    }
}
