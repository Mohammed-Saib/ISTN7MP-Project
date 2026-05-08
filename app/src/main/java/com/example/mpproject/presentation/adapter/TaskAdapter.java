package com.example.mpproject.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mpproject.R;
import com.example.mpproject.domain.model.Todo;

// [View] RecyclerView adapter; receives data from the ViewModel via submitList().
public class TaskAdapter extends ListAdapter<Todo, TaskAdapter.TaskViewHolder> {

    private final OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Todo todo);
        void onTaskCheckedChange(Todo todo, boolean isChecked);
    }

    public TaskAdapter(OnTaskClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Todo> DIFF_CALLBACK = new DiffUtil.ItemCallback<Todo>() {
        @Override
        public boolean areItemsTheSame(@NonNull Todo oldItem, @NonNull Todo newItem) {
            // UUID string comparison — stable identity even after updates
            return oldItem.getTodoId().equals(newItem.getTodoId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Todo oldItem, @NonNull Todo newItem) {
            return oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.isCompleted() == newItem.isCompleted();
        }
    };

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView descriptionTextView;
        private final CheckBox completedCheckBox;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.task_title);
            descriptionTextView = itemView.findViewById(R.id.task_description);
            completedCheckBox = itemView.findViewById(R.id.task_checkbox);
        }

        public void bind(final Todo todo, final OnTaskClickListener listener) {
            titleTextView.setText(todo.getTitle());
            descriptionTextView.setText(
                    todo.getDescription() != null ? todo.getDescription() : "No description");

            // Clear the listener before setting checked state to avoid a spurious callback
            completedCheckBox.setOnCheckedChangeListener(null);
            completedCheckBox.setChecked(todo.isCompleted());
            completedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                    listener.onTaskCheckedChange(todo, isChecked));

            itemView.setOnClickListener(v -> listener.onTaskClick(todo));
        }
    }
}
