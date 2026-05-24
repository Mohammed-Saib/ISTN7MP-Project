package com.example.mpproject.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mpproject.R;
import com.example.mpproject.domain.model.CalendarEvent;
import com.example.mpproject.domain.model.Module;
import com.example.mpproject.domain.model.Todo;
import com.example.mpproject.domain.repository.CalendarEventRepository;
import com.example.mpproject.domain.repository.ModuleRepository;
import com.example.mpproject.domain.repository.TodoRepository;
import com.example.mpproject.presentation.view.calendar.AgendaItem;
import com.example.mpproject.presentation.view.calendar.CalendarDayItem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

// [ViewModel] Drives the calendar screen.
// Combines CalendarEvent + Todo LiveData sources reactively via MediatorLiveData.
public class CalendarViewModel extends ViewModel {

    private final CalendarEventRepository eventRepo;
    private final TodoRepository todoRepo;
    private final String userId;

    // Month shown in the grid — changing this rebuilds gridItems
    private final MutableLiveData<Calendar> displayedMonth = new MutableLiveData<>();
    // Day highlighted in the grid and shown in the agenda
    private final MutableLiveData<Calendar> selectedDay = new MutableLiveData<>();
    // Event being edited; null means "add new"
    private final MutableLiveData<CalendarEvent> editingEvent = new MutableLiveData<>();

    // Raw repository streams
    private final LiveData<List<CalendarEvent>> allEvents;
    private final LiveData<List<Todo>> activeTodos;
    private final LiveData<List<Todo>> completedTodos;
    public  final LiveData<List<Module>> modules; // exposed for the module picker

    // Derived: 42 cells for the displayed month grid
    public final MediatorLiveData<List<CalendarDayItem>> gridItems = new MediatorLiveData<>();
    // Derived: agenda rows for the selected day
    public final MediatorLiveData<List<AgendaItem>> selectedDayItems = new MediatorLiveData<>();
    // Derived: all events + active todos from today onwards, sorted by time — used by the list view
    public final MediatorLiveData<List<AgendaItem>> upcomingItems = new MediatorLiveData<>();

    public CalendarViewModel(CalendarEventRepository eventRepo,
                             TodoRepository todoRepo,
                             ModuleRepository moduleRepo,
                             String userId) {
        this.eventRepo = eventRepo;
        this.todoRepo  = todoRepo;
        this.userId    = userId;

        allEvents      = eventRepo.getAllByUser(userId);
        activeTodos    = todoRepo.getAllByUser(userId);
        completedTodos = todoRepo.getCompletedByUser(userId);
        modules        = moduleRepo.getActiveByUser(userId);

        // Seed current date
        Calendar today = Calendar.getInstance();
        selectedDay.setValue(today);
        displayedMonth.setValue((Calendar) today.clone());

        // Agenda rebuilds when any source changes
        selectedDayItems.addSource(allEvents,      e -> rebuildAgenda());
        selectedDayItems.addSource(activeTodos,    t -> rebuildAgenda());
        selectedDayItems.addSource(completedTodos, t -> rebuildAgenda());
        selectedDayItems.addSource(selectedDay,    d -> rebuildAgenda());

        // Grid rebuilds when events, todos, month, or selected day changes
        gridItems.addSource(allEvents,      e -> rebuildGrid());
        gridItems.addSource(activeTodos,    t -> rebuildGrid());
        gridItems.addSource(completedTodos, t -> rebuildGrid());
        gridItems.addSource(displayedMonth, m -> rebuildGrid());
        gridItems.addSource(selectedDay,    d -> rebuildGrid()); // refreshes selection highlight

        // Upcoming list rebuilds when events or active todos change
        upcomingItems.addSource(allEvents,   e -> rebuildUpcoming());
        upcomingItems.addSource(activeTodos, t -> rebuildUpcoming());
    }

    // ── Public state accessors ───────────────────────────────────────────────

    public LiveData<Calendar> getDisplayedMonth() { return displayedMonth; }
    public LiveData<Calendar> getSelectedDay()    { return selectedDay; }
    public LiveData<CalendarEvent> getEditingEvent() { return editingEvent; }

    public void selectDay(Calendar day) {
        selectedDay.setValue(day);
    }

    public void navigateMonth(int delta) {
        Calendar c = (Calendar) displayedMonth.getValue().clone();
        c.add(Calendar.MONTH, delta);
        displayedMonth.setValue(c);
    }

    public void setEditingEvent(CalendarEvent event) {
        editingEvent.setValue(event);
    }

    // ── CRUD ────────────────────────────────────────────────────────────────

    public void addEvent(String title, String type, long startTime, Long endTime,
                         boolean allDay, String moduleId, String description,
                         String recurrencePattern, Long recurrenceEndDate) {
        if (recurrencePattern != null) {
            createRecurringEvents(title, type, startTime, endTime, allDay, moduleId, description,
                    recurrencePattern, recurrenceEndDate);
        } else {
            CalendarEvent e = new CalendarEvent(UUID.randomUUID().toString(), userId, title, startTime);
            e.setType(type);
            e.setEndTime(endTime);
            e.setAllDay(allDay);
            e.setModuleId(moduleId);
            e.setDescription(description);
            e.setUpdatedAt(System.currentTimeMillis());
            eventRepo.insert(e);
        }
    }

    public void updateEvent(CalendarEvent event) {
        event.setUpdatedAt(System.currentTimeMillis());
        if (event.getRecurrenceGroupId() != null) {
            eventRepo.updateGroup(event);
            eventRepo.update(event);
        } else {
            eventRepo.update(event);
        }
    }

    public void deleteEvent(CalendarEvent event) {
        eventRepo.delete(event);
    }

    public void deleteEventGroup(CalendarEvent event) {
        if (event.getRecurrenceGroupId() != null) {
            eventRepo.deleteGroup(event.getRecurrenceGroupId(), userId);
        } else {
            eventRepo.delete(event);
        }
    }

    // Called from AgendaAdapter when user checks/unchecks a todo
    public void toggleTodo(Todo todo, boolean checked) {
        todo.setCompleted(checked);
        todo.setCompletedAt(checked ? System.currentTimeMillis() : null);
        todoRepo.update(todo);
        if (checked && todo.getRecurrencePattern() != null && todo.getRecurrenceGroupId() != null) {
            scheduleNextTodoOccurrence(todo);
        }
    }

    // Pre-create all instances of a recurring event (eager strategy, capped at 100).
    // Collected into a list and inserted as a single batch to avoid 100 separate DB transactions.
    private void createRecurringEvents(String title, String type, long startTime, Long endTime,
                                       boolean allDay, String moduleId, String description,
                                       String recurrencePattern, Long recurrenceEndDate) {
        String groupId = UUID.randomUUID().toString();
        Calendar cur = Calendar.getInstance();
        cur.setTimeInMillis(startTime);
        long durationMs = (endTime != null) ? endTime - startTime : 0;
        long now = System.currentTimeMillis();
        int count = 0;

        List<CalendarEvent> batch = new ArrayList<>();
        while (count < 100) {
            long curStart = cur.getTimeInMillis();
            if (recurrenceEndDate != null && curStart > recurrenceEndDate) break;

            CalendarEvent e = new CalendarEvent(UUID.randomUUID().toString(), userId, title, curStart);
            e.setType(type);
            e.setEndTime(durationMs > 0 ? curStart + durationMs : null);
            e.setAllDay(allDay);
            e.setModuleId(moduleId);
            e.setDescription(description);
            e.setRecurrencePattern(recurrencePattern);
            e.setRecurrenceGroupId(groupId);
            e.setRecurrenceEndDate(recurrenceEndDate);
            e.setUpdatedAt(now);
            batch.add(e);
            count++;

            switch (recurrencePattern) {
                case "DAILY":   cur.add(Calendar.DAY_OF_YEAR, 1); break;
                case "WEEKLY":  cur.add(Calendar.WEEK_OF_YEAR, 1); break;
                case "MONTHLY": cur.add(Calendar.MONTH, 1); break;
                case "YEARLY":  cur.add(Calendar.YEAR, 1); break;
                default: break;
            }
        }
        if (!batch.isEmpty()) eventRepo.insertBatch(batch);
    }

    private void scheduleNextTodoOccurrence(Todo completed) {
        if (completed.getDueDate() == null) return;
        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(completed.getDueDate());
        switch (completed.getRecurrencePattern()) {
            case "DAILY":   next.add(Calendar.DAY_OF_YEAR, 1); break;
            case "WEEKLY":  next.add(Calendar.WEEK_OF_YEAR, 1); break;
            case "MONTHLY": next.add(Calendar.MONTH, 1); break;
            case "YEARLY":  next.add(Calendar.YEAR, 1); break;
            default: return;
        }
        long nextMs = next.getTimeInMillis();
        if (completed.getRecurrenceEndDate() != null && nextMs > completed.getRecurrenceEndDate()) return;

        Todo nextTodo = new Todo(UUID.randomUUID().toString(), completed.getUserId(), completed.getTitle());
        nextTodo.setDescription(completed.getDescription());
        nextTodo.setPriority(completed.getPriority());
        nextTodo.setDueDate(nextMs);
        nextTodo.setModuleId(completed.getModuleId());
        nextTodo.setRecurrencePattern(completed.getRecurrencePattern());
        nextTodo.setRecurrenceGroupId(completed.getRecurrenceGroupId());
        nextTodo.setRecurrenceEndDate(completed.getRecurrenceEndDate());
        todoRepo.insert(nextTodo);
    }

    // ── Private builders ────────────────────────────────────────────────────

    private void rebuildAgenda() {
        Calendar day = selectedDay.getValue();
        if (day == null) return;

        List<AgendaItem> items = new ArrayList<>();

        List<CalendarEvent> events = allEvents.getValue();
        if (events != null) {
            for (CalendarEvent e : events) {
                if (isSameDay(e.getStartTime(), day)) items.add(AgendaItem.fromEvent(e));
            }
        }

        // Combine active + completed todos so past due items still appear on their day
        addTodosForDay(activeTodos.getValue(), day, items);
        addTodosForDay(completedTodos.getValue(), day, items);

        Collections.sort(items, (a, b) -> Long.compare(a.getSortKey(), b.getSortKey()));
        selectedDayItems.setValue(items);
    }

    private void addTodosForDay(List<Todo> todos, Calendar day, List<AgendaItem> out) {
        if (todos == null) return;
        for (Todo t : todos) {
            if (t.getDueDate() != null && isSameDay(t.getDueDate(), day)) {
                out.add(AgendaItem.fromTodo(t));
            }
        }
    }

    private void rebuildGrid() {
        Calendar month = displayedMonth.getValue();
        Calendar selected = selectedDay.getValue();
        if (month == null) return;

        List<CalendarDayItem> cells = new ArrayList<>(42);

        Calendar firstDay = (Calendar) month.clone();
        firstDay.set(Calendar.DAY_OF_MONTH, 1);
        // DAY_OF_WEEK: 1=Sun … 7=Sat; convert to 0-based leading padding count
        int leadingPad = firstDay.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar today = Calendar.getInstance();

        // Padding cells before the 1st
        for (int i = 0; i < leadingPad; i++) {
            cells.add(new CalendarDayItem(0, false, false, false, Collections.emptyList(), false));
        }

        // One cell per day
        for (int d = 1; d <= daysInMonth; d++) {
            Calendar dayCal = (Calendar) firstDay.clone();
            dayCal.set(Calendar.DAY_OF_MONTH, d);

            boolean isToday    = isSameDay(today.getTimeInMillis(), dayCal);
            boolean isSel      = selected != null && isSameDay(selected.getTimeInMillis(), dayCal);
            List<Integer> dots = buildDots(dayCal);
            boolean hasTodo    = hasTodoOnDay(dayCal);

            cells.add(new CalendarDayItem(d, true, isToday, isSel, dots, hasTodo));
        }

        // Trailing padding to always fill 6 rows × 7 columns = 42
        while (cells.size() < 42) {
            cells.add(new CalendarDayItem(0, false, false, false, Collections.emptyList(), false));
        }

        gridItems.setValue(cells);
    }

    // Collect up to 3 event-type color resource IDs for a given day (resolved to int in adapter)
    private List<Integer> buildDots(Calendar day) {
        List<Integer> dots = new ArrayList<>(3);
        List<CalendarEvent> events = allEvents.getValue();
        if (events == null) return dots;
        for (CalendarEvent e : events) {
            if (dots.size() >= 3) break;
            if (isSameDay(e.getStartTime(), day)) dots.add(colorResForType(e.getType()));
        }
        return dots;
    }

    private boolean hasTodoOnDay(Calendar day) {
        if (hasMatchingTodo(activeTodos.getValue(), day)) return true;
        return hasMatchingTodo(completedTodos.getValue(), day);
    }

    private boolean hasMatchingTodo(List<Todo> todos, Calendar day) {
        if (todos == null) return false;
        for (Todo t : todos) {
            if (t.getDueDate() != null && isSameDay(t.getDueDate(), day)) return true;
        }
        return false;
    }

    // Builds the upcoming list: events and active todos from today midnight onwards, sorted by time.
    private void rebuildUpcoming() {
        long todayStart = getTodayStart();
        List<AgendaItem> items = new ArrayList<>();

        List<CalendarEvent> events = allEvents.getValue();
        if (events != null) {
            for (CalendarEvent e : events) {
                if (e.getStartTime() >= todayStart) items.add(AgendaItem.fromEvent(e));
            }
        }

        List<Todo> todos = activeTodos.getValue();
        if (todos != null) {
            for (Todo t : todos) {
                if (t.getDueDate() != null && t.getDueDate() >= todayStart) items.add(AgendaItem.fromTodo(t));
            }
        }

        Collections.sort(items, (a, b) -> Long.compare(a.getSortKey(), b.getSortKey()));
        upcomingItems.setValue(items);
    }

    private long getTodayStart() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private boolean isSameDay(long millis, Calendar day) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        return c.get(Calendar.YEAR)         == day.get(Calendar.YEAR)
            && c.get(Calendar.MONTH)        == day.get(Calendar.MONTH)
            && c.get(Calendar.DAY_OF_MONTH) == day.get(Calendar.DAY_OF_MONTH);
    }

    // Maps an event type string to a color resource int (resolved later by the adapter)
    public static int colorResForType(String type) {
        if (type == null) return R.color.event_lecture;
        switch (type) {
            case "EXAM":            return R.color.event_exam;
            case "ASSIGNMENT_DUE":  return R.color.event_assignment;
            case "PERSONAL":        return R.color.event_personal;
            case "QUIZ":            return R.color.event_quiz;
            case "TEST":            return R.color.event_test;
            case "LAB":             return R.color.event_lab;
            default:                return R.color.event_lecture; // LECTURE + OTHER
        }
    }

}
