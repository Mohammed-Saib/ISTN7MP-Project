package com.example.mpproject.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MyTaskAdapter extends RecyclerView.Adapter<MyTaskAdapter.VH> {

    public interface OnRemoveListener { void onRemove(int index); }
    public interface OnSelectListener { void onSelect(String label); }

    private List<String> items;
    private final OnRemoveListener onRemove;
    private final OnSelectListener onSelect;

    public MyTaskAdapter(List<String> items, OnRemoveListener onRemove, OnSelectListener onSelect) {
        this.items    = new ArrayList<>(items);
        this.onRemove = onRemove;
        this.onSelect = onSelect;
    }

    public void updateList(List<String> newItems) {
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
        String label = items.get(position);
        holder.title.setText(label);
        holder.subtitle.setText("Tap to focus · ✕ to remove");
        holder.itemView.setOnClickListener(v -> onSelect.onSelect(label));
        holder.itemView.setOnLongClickListener(v -> {
            onRemove.onRemove(position);
            return true;
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;
        VH(View v) { super(v); title = v.findViewById(android.R.id.text1); subtitle = v.findViewById(android.R.id.text2); }
    }
}