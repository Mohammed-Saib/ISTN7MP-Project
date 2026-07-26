package com.example.mpproject.presentation.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mpproject.R;
import com.example.mpproject.domain.model.CalendarEvent;
import com.example.mpproject.domain.model.Todo;
import com.example.mpproject.presentation.view.calendar.AgendaItem;
import com.example.mpproject.presentation.viewmodel.CalendarViewModel;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

// [Adapter] Multi-type RecyclerView for the day agenda and upcoming list.
// Two view types: CalendarEvent (TYPE_EVENT) and Todo (TYPE_TODO).
public class AgendaAdapter extends ListAdapter<AgendaItem, RecyclerView.ViewHolder> {

    public interface OnEventClickListener  { void onEventClick(CalendarEvent event); }
    public interface OnTodoCheckedListener { void onTodoChecked(Todo todo, boolean checked); }

    // Resolved at bind time so module chips populate correctly for off-screen items too
    private Map<String, String> moduleNameMap = new HashMap<>();
    // Show event date in list/upcoming mode; hidden in day-agenda mode (date already in header)
    private boolean showDate = false;

    private final OnEventClickListener  eventClickListener;
    private final OnTodoCheckedListener todoCheckedListener;

    // Static formatters — created once, reused across all bind calls
    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("EEE, d MMM", Locale.getDefault());

    private static final DiffUtil.ItemCallback<AgendaItem> DIFF =
            new DiffUtil.ItemCallback<AgendaItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull AgendaItem oldItem, @NonNull AgendaItem newItem) {
                    if (oldItem.getType() != newItem.getType()) return false;
                    if (oldItem.getType() == AgendaItem.TYPE_EVENT) {
                        return oldItem.getEvent().getEventId().equals(newItem.getEvent().getEventId());
                    }
                    return oldItem.getTodo().getTodoId().equals(newItem.getTodo().getTodoId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull AgendaItem oldItem, @NonNull AgendaItem newItem) {
                    if (oldItem.getType() != newItem.getType()) return false;
                    if (oldItem.getType() == AgendaItem.TYPE_EVENT) {
                        CalendarEvent o = oldItem.getEvent();
                        CalendarEvent n = newItem.getEvent();
                        return o.getEventId().equals(n.getEventId())
                                && Objects.equals(o.getTitle(), n.getTitle())
                                && Objects.equals(o.getType(), n.getType())
                                && o.getStartTime() == n.getStartTime()
                                && Objects.equals(o.getEndTime(), n.getEndTime())
                                && o.isAllDay() == n.isAllDay()
                                && Objects.equals(o.getModuleId(), n.getModuleId())
                                && Objects.equals(o.getRecurrenceGroupId(), n.getRecurrenceGroupId());
                    }
                    Todo o = oldItem.getTodo();
                    Todo n = newItem.getTodo();
                    return o.getTodoId().equals(n.getTodoId())
                            && Objects.equals(o.getTitle(), n.getTitle())
                            && o.isCompleted() == n.isCompleted()
                            && Objects.equals(o.getPriority(), n.getPriority())
                            && Objects.equals(o.getDueDate(), n.getDueDate())
                            && Objects.equals(o.getModuleId(), n.getModuleId());
                }
            };

    public AgendaAdapter(OnEventClickListener eventClick, OnTodoCheckedListener todoChecked) {
        super(DIFF);
        this.eventClickListener  = eventClick;
        this.todoCheckedListener = todoChecked;
    }

    public void setModuleNameMap(Map<String, String> map) {
        moduleNameMap = map != null ? map : new HashMap<>();
    }

    public void setShowDate(boolean show) {
        showDate = show;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == AgendaItem.TYPE_EVENT) {
            return new EventViewHolder(inf.inflate(R.layout.item_agenda_event, parent, false));
        }
        return new TodoViewHolder(inf.inflate(R.layout.item_agenda_todo, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AgendaItem item = getItem(position);
        if (item.getType() == AgendaItem.TYPE_EVENT) {
            ((EventViewHolder) holder).bind(item.getEvent());
        } else {
            ((TodoViewHolder) holder).bind(item.getTodo());
        }
    }

    // ── Event view holder ──────────────────────────────────────────────────

    class EventViewHolder extends RecyclerView.ViewHolder {

        private final View      typeStrip;
        private final TextView  tvTitle;
        private final TextView  tvDate;
        private final TextView  tvTime;
        private final TextView  tvModule;
        private final ImageView ivRecurring;
        // Reused across bind calls — avoids allocation per scroll frame
        private final GradientDrawable stripDrawable = new GradientDrawable();

        EventViewHolder(@NonNull View v) {
            super(v);
            typeStrip   = v.findViewById(R.id.view_type_strip);
            tvTitle     = v.findViewById(R.id.tv_event_title);
            tvDate      = v.findViewById(R.id.tv_event_date);
            tvTime      = v.findViewById(R.id.tv_event_time);
            tvModule    = v.findViewById(R.id.tv_event_module);
            ivRecurring = v.findViewById(R.id.iv_recurring);
            typeStrip.setBackground(stripDrawable);
        }

        void bind(CalendarEvent e) {
            Context ctx = itemView.getContext();

            // Left strip colour by event type — mutate existing drawable instead of creating new
            int colorInt = ContextCompat.getColor(ctx, CalendarViewModel.colorResForType(e.getType()));
            stripDrawable.setColor(colorInt);

            tvTitle.setText(e.getTitle());

            // Date shown only in list/upcoming mode
            if (showDate) {
                tvDate.setVisibility(View.VISIBLE);
                tvDate.setText(DATE_FMT.format(new Date(e.getStartTime())));
            } else {
                tvDate.setVisibility(View.GONE);
            }

            // Time: "All day" or "HH:mm – HH:mm"
            if (e.isAllDay()) {
                tvTime.setText("All day");
            } else {
                String start = TIME_FMT.format(new Date(e.getStartTime()));
                tvTime.setText(e.getEndTime() != null
                        ? start + " – " + TIME_FMT.format(new Date(e.getEndTime()))
                        : start);
            }

            // Module chip resolved from map at bind time (handles off-screen items correctly)
            String moduleId = e.getModuleId();
            if (moduleId != null && !moduleId.isEmpty()) {
                String name = moduleNameMap.get(moduleId);
                if (name != null) {
                    tvModule.setText(name);
                    tvModule.setVisibility(View.VISIBLE);
                } else {
                    tvModule.setVisibility(View.GONE);
                }
            } else {
                tvModule.setVisibility(View.GONE);
            }

            ivRecurring.setVisibility(e.getRecurrenceGroupId() != null ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> eventClickListener.onEventClick(e));
        }
    }

    // ── Todo view holder ───────────────────────────────────────────────────

    class TodoViewHolder extends RecyclerView.ViewHolder {

        private final View     priorityStrip;
        private final MaterialCheckBox cbTodo;
        private final TextView tvTitle;
        private final TextView tvTime;
        private final TextView tvModule;
        // Reused across bind calls — avoids allocation per scroll frame
        private final GradientDrawable stripDrawable = new GradientDrawable();

        TodoViewHolder(@NonNull View v) {
            super(v);
            priorityStrip = v.findViewById(R.id.view_priority_strip);
            cbTodo        = v.findViewById(R.id.cb_todo);
            tvTitle       = v.findViewById(R.id.tv_todo_title);
            tvTime        = v.findViewById(R.id.tv_todo_time);
            tvModule      = v.findViewById(R.id.tv_todo_module);
            priorityStrip.setBackground(stripDrawable);
        }

        void bind(Todo todo) {
            Context ctx = itemView.getContext();

            // Priority strip — mutate existing drawable instead of creating new
            int priorityColor;
            switch (todo.getPriority() != null ? todo.getPriority() : "MEDIUM") {
                case "HIGH":  priorityColor = R.color.priority_high;   break;
                case "LOW":   priorityColor = R.color.priority_low;    break;
                default:      priorityColor = R.color.priority_medium; break;
            }
            stripDrawable.setColor(ContextCompat.getColor(ctx, priorityColor));

            // Suppress listener during bind to avoid spurious callbacks
            cbTodo.setOnCheckedChangeListener(null);
            cbTodo.setChecked(todo.isCompleted());
            cbTodo.setOnCheckedChangeListener((btn, checked) ->
                    todoCheckedListener.onTodoChecked(todo, checked));

            // Strike-through completed todos
            tvTitle.setText(todo.getTitle());
            tvTitle.setPaintFlags(todo.isCompleted()
                    ? tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                    : tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

            // Show due time only if the dueDate has a non-midnight time component
            if (todo.getDueDate() != null) {
                String time = TIME_FMT.format(new Date(todo.getDueDate()));
                if (!time.equals("00:00")) {
                    tvTime.setVisibility(View.VISIBLE);
                    tvTime.setText(time);
                } else {
                    tvTime.setVisibility(View.GONE);
                }
            } else {
                tvTime.setVisibility(View.GONE);
            }

            // Module chip resolved from map at bind time
            String moduleId = todo.getModuleId();
            if (moduleId != null && !moduleId.isEmpty()) {
                String name = moduleNameMap.get(moduleId);
                if (name != null) {
                    tvModule.setText(name);
                    tvModule.setVisibility(View.VISIBLE);
                } else {
                    tvModule.setVisibility(View.GONE);
                }
            } else {
                tvModule.setVisibility(View.GONE);
            }
        }
    }
}
