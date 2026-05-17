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
import androidx.recyclerview.widget.RecyclerView;

import com.example.mpproject.R;
import com.example.mpproject.domain.model.CalendarEvent;
import com.example.mpproject.domain.model.Todo;
import com.example.mpproject.presentation.view.calendar.AgendaItem;
import com.example.mpproject.presentation.viewmodel.CalendarViewModel;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// [Adapter] Multi-type RecyclerView for the day's agenda.
// Two view types: CalendarEvent (TYPE_EVENT) and Todo (TYPE_TODO).
public class AgendaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnEventClickListener  { void onEventClick(CalendarEvent event); }
    public interface OnTodoCheckedListener { void onTodoChecked(Todo todo, boolean checked); }

    private List<AgendaItem> items = new ArrayList<>();
    private final OnEventClickListener  eventClickListener;
    private final OnTodoCheckedListener todoCheckedListener;

    private static final SimpleDateFormat TIME_FMT =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    public AgendaAdapter(OnEventClickListener eventClick, OnTodoCheckedListener todoChecked) {
        this.eventClickListener  = eventClick;
        this.todoCheckedListener = todoChecked;
    }

    public void submitList(List<AgendaItem> newItems) {
        items = newItems != null ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType();
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
        AgendaItem item = items.get(position);
        if (item.getType() == AgendaItem.TYPE_EVENT) {
            ((EventViewHolder) holder).bind(item.getEvent());
        } else {
            ((TodoViewHolder) holder).bind(item.getTodo());
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── Event view holder ──────────────────────────────────────────────────

    class EventViewHolder extends RecyclerView.ViewHolder {

        private final View      typeStrip;
        private final TextView  tvTitle;
        private final TextView  tvTime;
        private final TextView  tvModule;
        private final TextView  tvType;
        private final ImageView ivRecurring;

        EventViewHolder(@NonNull View v) {
            super(v);
            typeStrip   = v.findViewById(R.id.view_type_strip);
            tvTitle     = v.findViewById(R.id.tv_event_title);
            tvTime      = v.findViewById(R.id.tv_event_time);
            tvModule    = v.findViewById(R.id.tv_event_module);
            tvType      = v.findViewById(R.id.tv_event_type);
            ivRecurring = v.findViewById(R.id.iv_recurring);
        }

        void bind(CalendarEvent e) {
            Context ctx = itemView.getContext();

            // Left strip colour by type
            int colorInt = ContextCompat.getColor(ctx, CalendarViewModel.colorResForType(e.getType()));
            GradientDrawable strip = new GradientDrawable();
            strip.setColor(colorInt);
            typeStrip.setBackground(strip);

            tvTitle.setText(e.getTitle());

            // Time: "All day" or "HH:mm – HH:mm"
            if (e.isAllDay()) {
                tvTime.setText("All day");
            } else {
                String start = TIME_FMT.format(new Date(e.getStartTime()));
                tvTime.setText(e.getEndTime() != null
                        ? start + " – " + TIME_FMT.format(new Date(e.getEndTime()))
                        : start);
            }

            // Module chip
            if (e.getModuleId() != null && !e.getModuleId().isEmpty()) {
                tvModule.setVisibility(View.VISIBLE);
                // Module name resolved by CalendarFragment via the modules LiveData
                tvModule.setTag(e.getModuleId());
            } else {
                tvModule.setVisibility(View.GONE);
            }

            // Type badge label
            tvType.setText(labelForType(e.getType()));
            tvType.setTextColor(colorInt);

            // Recurring indicator
            ivRecurring.setVisibility(e.getRecurrenceGroupId() != null ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> eventClickListener.onEventClick(e));
        }

        private String labelForType(String type) {
            if (type == null) return "";
            switch (type) {
                case "EXAM":           return "EXAM";
                case "ASSIGNMENT_DUE": return "DUE";
                case "PERSONAL":       return "PERSONAL";
                default:               return "LECTURE";
            }
        }
    }

    // ── Todo view holder ───────────────────────────────────────────────────

    class TodoViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCheckBox cbTodo;
        private final TextView tvTitle;
        private final TextView tvTime;
        private final View priorityDot;

        TodoViewHolder(@NonNull View v) {
            super(v);
            cbTodo      = v.findViewById(R.id.cb_todo);
            tvTitle     = v.findViewById(R.id.tv_todo_title);
            tvTime      = v.findViewById(R.id.tv_todo_time);
            priorityDot = v.findViewById(R.id.view_priority_dot);
        }

        void bind(Todo todo) {
            Context ctx = itemView.getContext();

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

            // Priority dot colour
            int priorityColor;
            switch (todo.getPriority() != null ? todo.getPriority() : "MEDIUM") {
                case "HIGH":  priorityColor = R.color.priority_high;   break;
                case "LOW":   priorityColor = R.color.priority_low;    break;
                default:      priorityColor = R.color.priority_medium; break;
            }
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(ContextCompat.getColor(ctx, priorityColor));
            priorityDot.setBackground(dot);
        }
    }
}
