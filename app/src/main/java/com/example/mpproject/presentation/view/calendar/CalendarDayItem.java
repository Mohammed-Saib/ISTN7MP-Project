package com.example.mpproject.presentation.view.calendar;

import java.util.List;

// [Model] Immutable data for one cell in the month grid.
// dayOfMonth == 0 means a padding cell (before/after the month's days).
public class CalendarDayItem {

    private final int dayOfMonth;
    private final boolean isCurrentMonth;
    private final boolean isToday;
    private final boolean isSelected;
    // Up to 3 resolved color ints for event-type dots (empty = no events)
    private final List<Integer> eventDotColors;
    // True if any active todo is due on this day
    private final boolean hasTodo;

    public CalendarDayItem(int dayOfMonth, boolean isCurrentMonth,
                           boolean isToday, boolean isSelected,
                           List<Integer> eventDotColors, boolean hasTodo) {
        this.dayOfMonth = dayOfMonth;
        this.isCurrentMonth = isCurrentMonth;
        this.isToday = isToday;
        this.isSelected = isSelected;
        this.eventDotColors = eventDotColors;
        this.hasTodo = hasTodo;
    }

    public int getDayOfMonth()         { return dayOfMonth; }
    public boolean isCurrentMonth()    { return isCurrentMonth; }
    public boolean isToday()           { return isToday; }
    public boolean isSelected()        { return isSelected; }
    public List<Integer> getEventDotColors() { return eventDotColors; }
    public boolean hasTodo()           { return hasTodo; }
    public boolean isPadding()         { return dayOfMonth == 0; }
}
