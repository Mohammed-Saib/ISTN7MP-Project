package com.example.mpproject.presentation.view.calendar;

import com.example.mpproject.domain.model.CalendarEvent;
import com.example.mpproject.domain.model.Todo;

// [Model] Union type for the day-agenda list — either a CalendarEvent or a due-date Todo.
// The multi-type AgendaAdapter uses getType() to inflate the correct view holder.
public class AgendaItem {

    public static final int TYPE_EVENT = 0;
    public static final int TYPE_TODO  = 1;

    private final int type;
    private final CalendarEvent event; // non-null when type == TYPE_EVENT
    private final Todo todo;           // non-null when type == TYPE_TODO

    private AgendaItem(int type, CalendarEvent event, Todo todo) {
        this.type  = type;
        this.event = event;
        this.todo  = todo;
    }

    public static AgendaItem fromEvent(CalendarEvent event) {
        return new AgendaItem(TYPE_EVENT, event, null);
    }

    public static AgendaItem fromTodo(Todo todo) {
        return new AgendaItem(TYPE_TODO, null, todo);
    }

    public int getType()          { return type; }
    public CalendarEvent getEvent() { return event; }
    public Todo getTodo()           { return todo; }

    // Used to sort the agenda list chronologically
    public long getSortKey() {
        if (type == TYPE_EVENT) return event.getStartTime();
        return todo.getDueDate() != null ? todo.getDueDate() : Long.MAX_VALUE;
    }
}
