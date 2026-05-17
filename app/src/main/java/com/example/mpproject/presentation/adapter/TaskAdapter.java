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
import com.example.mpproject.domain.model.Todo;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// [Adapter] Todo list — shows priority dot, due date, module chip, strike-through for completed.
public class TaskAdapter extends ListAdapter<Todo, TaskAdapter.TaskViewHolder> {

    public interface OnTaskClickListener {
        void onTaskClick(Todo todo);
        void onTaskCheckedChange(Todo todo, boolean isChecked);
    }

    private final OnTaskClickListener listener;
    private Map<String, String> moduleNameMap = new HashMap<>();

    private static final SimpleDateFormat DUE_FMT =
            new SimpleDateFormat("EEE, d MMM HH:mm", Locale.getDefault());

    public TaskAdapter(OnTaskClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setModuleNameMap(Map<String, String> map) {
        moduleNameMap = map != null ? map : new HashMap<>();
        notifyDataSetChanged();
    }

    private static final DiffUtil.ItemCallback<Todo> DIFF_CALLBACK = new DiffUtil.ItemCallback<Todo>() {
        @Override
        public boolean areItemsTheSame(@NonNull Todo o, @NonNull Todo n) {
            return o.getTodoId().equals(n.getTodoId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Todo o, @NonNull Todo n) {
            return o.getTitle().equals(n.getTitle())
                    && o.isCompleted() == n.isCompleted()
                    && o.getPriority().equals(n.getPriority())
                    && java.util.Objects.equals(o.getDueDate(), n.getDueDate())
                    && java.util.Objects.equals(o.getModuleId(), n.getModuleId());
        }
    };

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TaskViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(getItem(position), listener, moduleNameMap);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCheckBox checkbox;
        private final View priorityDot;
        private final TextView tvTitle;
        private final TextView tvDueDate;
        private final TextView tvModule;
        private final ImageView ivRecurring;

        TaskViewHolder(@NonNull View v) {
            super(v);
            checkbox    = v.findViewById(R.id.task_checkbox);
            priorityDot = v.findViewById(R.id.view_priority_dot);
            tvTitle     = v.findViewById(R.id.task_title);
            tvDueDate   = v.findViewById(R.id.tv_due_date);
            tvModule    = v.findViewById(R.id.tv_task_module);
            ivRecurring = v.findViewById(R.id.iv_recurring);
        }

        void bind(Todo todo, OnTaskClickListener listener, Map<String, String> moduleNameMap) {
            Context ctx = itemView.getContext();

            // Priority dot colour
            int dotColorRes;
            switch (todo.getPriority() != null ? todo.getPriority() : "MEDIUM") {
                case "HIGH":  dotColorRes = R.color.priority_high;   break;
                case "LOW":   dotColorRes = R.color.priority_low;    break;
                default:      dotColorRes = R.color.priority_medium; break;
            }
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(ContextCompat.getColor(ctx, dotColorRes));
            priorityDot.setBackground(dot);

            // Title + strike-through for completed
            tvTitle.setText(todo.getTitle());
            tvTitle.setPaintFlags(todo.isCompleted()
                    ? tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                    : tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

            // Due date
            if (todo.getDueDate() != null) {
                String dueText = DUE_FMT.format(new Date(todo.getDueDate()));
                // Hide time component when it's exactly midnight (date-only tasks)
                if (dueText.endsWith("00:00")) {
                    dueText = dueText.substring(0, dueText.length() - 6);
                }
                tvDueDate.setText("Due: " + dueText);
                tvDueDate.setVisibility(View.VISIBLE);
            } else {
                tvDueDate.setVisibility(View.GONE);
            }

            // Module chip — resolved at bind time from map, no post-layout walk needed
            if (todo.getModuleId() != null && !todo.getModuleId().isEmpty()) {
                String moduleName = moduleNameMap.get(todo.getModuleId());
                if (moduleName != null) {
                    tvModule.setText(moduleName);
                    tvModule.setVisibility(View.VISIBLE);
                } else {
                    tvModule.setVisibility(View.GONE);
                }
            } else {
                tvModule.setVisibility(View.GONE);
            }

            // Recurring indicator
            ivRecurring.setVisibility(todo.getRecurrenceGroupId() != null ? View.VISIBLE : View.GONE);

            // Suppress listener during bind to avoid spurious callbacks
            checkbox.setOnCheckedChangeListener(null);
            checkbox.setChecked(todo.isCompleted());
            checkbox.setOnCheckedChangeListener((btn, checked) ->
                    listener.onTaskCheckedChange(todo, checked));

            itemView.setOnClickListener(v -> listener.onTaskClick(todo));
        }
    }
}
