package com.example.mpproject.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mpproject.R;
import com.example.mpproject.domain.model.Todo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionTaskAdapter extends RecyclerView.Adapter<SessionTaskAdapter.VH> {

    public interface OnTapListener  { void onTap(int position); }
    public interface OnLongListener { void onLong(int position); }

    // Mode A: recommended Todos
    private List<Todo> todoItems = new ArrayList<>();
    // Mode B: plain string labels (My Session List)
    private List<String> labelItems = new ArrayList<>();

    private final boolean isTodoMode; // true = recommended, false = labels
    private OnTapListener  tapListener;
    private OnLongListener longListener;

    public SessionTaskAdapter(boolean isTodoMode) {
        this.isTodoMode = isTodoMode;
    }

    public void setTapListener(OnTapListener l)  { this.tapListener  = l; }
    public void setLongListener(OnLongListener l) { this.longListener = l; }

    public void updateTodos(List<Todo> todos) {
        this.todoItems = todos != null ? new ArrayList<>(todos) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void updateLabels(List<String> labels) {
        this.labelItems = labels != null ? new ArrayList<>(labels) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.session_task_item, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        if (isTodoMode) {
            Todo todo = todoItems.get(position);
            holder.title.setText(todo.getTitle());

            String due = todo.getDueDate() != null
                    ? "Due " + new SimpleDateFormat("MMM d", Locale.getDefault())
                    .format(new Date(todo.getDueDate()))
                    : "No due date";
            holder.meta.setText(due);

            String p = todo.getPriority() != null ? todo.getPriority() : "";
            holder.badge.setText(p);
            holder.badge.setVisibility(p.isEmpty() ? View.GONE : View.VISIBLE);
            switch (p) {
                case "HIGH":   holder.badge.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFDC2626)); break;
                case "MEDIUM": holder.badge.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFD97706)); break;
                default:       holder.badge.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF16A34A)); break;
            }
        } else {
            holder.title.setText(labelItems.get(position));
            holder.meta.setVisibility(View.GONE);
            holder.badge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (tapListener != null) tapListener.onTap(holder.getAdapterPosition());
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (longListener != null) longListener.onLong(holder.getAdapterPosition());
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return isTodoMode ? todoItems.size() : labelItems.size();
    }

    public String getLabelAt(int pos)  { return labelItems.get(pos); }
    public Todo   getTodoAt(int pos)   { return todoItems.get(pos); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, meta, badge;
        VH(View v) {
            super(v);
            title = v.findViewById(R.id.tvTaskTitle);
            meta  = v.findViewById(R.id.tvTaskMeta);
            badge = v.findViewById(R.id.tvPriorityBadge);
        }
    }
}