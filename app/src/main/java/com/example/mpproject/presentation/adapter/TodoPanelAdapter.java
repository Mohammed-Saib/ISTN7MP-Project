package com.example.mpproject.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mpproject.data.local.entity.TodoEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TodoPanelAdapter extends RecyclerView.Adapter<TodoPanelAdapter.VH> {

    public interface OnTaskClickListener {
        void onTaskClick(TodoEntity todo);
    }

    private List<TodoEntity> items;
    private final OnTaskClickListener listener;

    public TodoPanelAdapter(List<TodoEntity> items, OnTaskClickListener listener) {
        this.items    = new ArrayList<>(items);
        this.listener = listener;
    }

    public void updateList(List<TodoEntity> newItems) {
        this.items = new ArrayList<>(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TodoEntity todo = items.get(position);
        holder.title.setText(todo.getTitle());

        // Subtitle: priority badge + due date
        String priority = todo.getPriority() != null ? todo.getPriority() : "—";
        String due = "No due date";
        if (todo.getDueDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
            due = "Due " + sdf.format(new Date(todo.getDueDate()));
        }
        holder.subtitle.setText(priority + "  ·  " + due);
        holder.itemView.setOnClickListener(v -> listener.onTaskClick(todo));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        VH(View v) {
            super(v);
            title    = v.findViewById(android.R.id.text1);
            subtitle = v.findViewById(android.R.id.text2);
        }
    }
}