package com.example.mpproject.presentation.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mpproject.R;
import com.example.mpproject.domain.model.Module;

// [Adapter] Module list — color accent strip, name, code/semester, lecturer, archived badge.
public class ModuleAdapter extends ListAdapter<Module, ModuleAdapter.ModuleViewHolder> {

    public interface OnModuleClickListener { void onModuleClick(Module module); }

    private final OnModuleClickListener listener;

    public ModuleAdapter(OnModuleClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Module> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Module>() {
                @Override
                public boolean areItemsTheSame(@NonNull Module o, @NonNull Module n) {
                    return o.getModuleId().equals(n.getModuleId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Module o, @NonNull Module n) {
                    return o.getName().equals(n.getName())
                            && java.util.Objects.equals(o.getModuleCode(), n.getModuleCode())
                            && java.util.Objects.equals(o.getColor(), n.getColor())
                            && java.util.Objects.equals(o.getSemester(), n.getSemester())
                            && java.util.Objects.equals(o.getLecturerName(), n.getLecturerName())
                            && o.isArchived() == n.isArchived();
                }
            };

    @NonNull
    @Override
    public ModuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ModuleViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_module, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ModuleViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ModuleViewHolder extends RecyclerView.ViewHolder {
        private final View colorStrip;
        private final TextView tvName;
        private final TextView tvMeta;
        private final TextView tvLecturer;
        private final TextView tvArchivedBadge;

        ModuleViewHolder(@NonNull View v) {
            super(v);
            colorStrip      = v.findViewById(R.id.view_color_strip);
            tvName          = v.findViewById(R.id.tv_module_name);
            tvMeta          = v.findViewById(R.id.tv_module_meta);
            tvLecturer      = v.findViewById(R.id.tv_lecturer_name);
            tvArchivedBadge = v.findViewById(R.id.tv_archived_badge);
        }

        void bind(Module m, OnModuleClickListener listener) {
            // Colour strip — fall back to a neutral grey if no color set
            try {
                colorStrip.setBackgroundColor(
                        m.getColor() != null ? Color.parseColor(m.getColor()) : 0xFF9E9E9E);
            } catch (IllegalArgumentException e) {
                colorStrip.setBackgroundColor(0xFF9E9E9E);
            }

            tvName.setText(m.getName());

            // "CS301 · Semester 1 2026" — omit parts that are null
            StringBuilder meta = new StringBuilder();
            if (m.getModuleCode() != null && !m.getModuleCode().isEmpty())
                meta.append(m.getModuleCode());
            if (m.getSemester() != null && !m.getSemester().isEmpty()) {
                if (meta.length() > 0) meta.append(" · ");
                meta.append(m.getSemester());
            }
            tvMeta.setText(meta.toString());
            tvMeta.setVisibility(meta.length() > 0 ? View.VISIBLE : View.GONE);

            if (m.getLecturerName() != null && !m.getLecturerName().isEmpty()) {
                tvLecturer.setText(m.getLecturerName());
                tvLecturer.setVisibility(View.VISIBLE);
            } else {
                tvLecturer.setVisibility(View.GONE);
            }

            tvArchivedBadge.setVisibility(m.isArchived() ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> listener.onModuleClick(m));
        }
    }
}
