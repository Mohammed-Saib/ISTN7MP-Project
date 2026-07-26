package com.example.mpproject.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mpproject.R;
import com.example.mpproject.data.local.entity.ResearchPaperEntity;

import java.util.Objects;

public class ResearchPaperAdapter extends ListAdapter<ResearchPaperEntity, ResearchPaperAdapter.ResearchPaperViewHolder> {

    public interface OnResearchPaperClickListener {
        void onPaperClick(ResearchPaperEntity paper);
        void onPaperLongClick(ResearchPaperEntity paper);
    }

    private static final DiffUtil.ItemCallback<ResearchPaperEntity> DIFF =
            new DiffUtil.ItemCallback<ResearchPaperEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull ResearchPaperEntity oldItem, @NonNull ResearchPaperEntity newItem) {
                    return Objects.equals(oldItem.getPaperId(), newItem.getPaperId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ResearchPaperEntity oldItem, @NonNull ResearchPaperEntity newItem) {
                    return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                            && Objects.equals(oldItem.getAuthors(), newItem.getAuthors())
                            && Objects.equals(oldItem.getYear(), newItem.getYear())
                            && Objects.equals(oldItem.getCategory(), newItem.getCategory())
                            && Objects.equals(oldItem.getStatus(), newItem.getStatus())
                            && oldItem.isImportant() == newItem.isImportant()
                            && Objects.equals(oldItem.getSummary(), newItem.getSummary());
                }
            };

    private final OnResearchPaperClickListener listener;

    public ResearchPaperAdapter(OnResearchPaperClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ResearchPaperViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_research_paper, parent, false);
        return new ResearchPaperViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResearchPaperViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ResearchPaperViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtPaperTitle;
        private final TextView txtPaperMeta;
        private final TextView txtPaperCategory;
        private final TextView txtPaperSummary;
        private final TextView txtPaperStatus;

        public ResearchPaperViewHolder(@NonNull View itemView) {
            super(itemView);

            txtPaperTitle = itemView.findViewById(R.id.txtPaperTitle);
            txtPaperMeta = itemView.findViewById(R.id.txtPaperMeta);
            txtPaperCategory = itemView.findViewById(R.id.txtPaperCategory);
            txtPaperSummary = itemView.findViewById(R.id.txtPaperSummary);
            txtPaperStatus = itemView.findViewById(R.id.txtPaperStatus);
        }

        void bind(ResearchPaperEntity paper) {
            txtPaperTitle.setText(paper.getTitle().isEmpty() ? "Untitled Paper" : paper.getTitle());

            String authors = paper.getAuthors().isEmpty() ? "Unknown author" : paper.getAuthors();
            String year = paper.getYear().isEmpty() ? "No year" : paper.getYear();
            txtPaperMeta.setText(authors + " \u2022 " + year);

            String category = paper.getCategory().isEmpty() ? "No category" : paper.getCategory();
            txtPaperCategory.setText("Theme: " + category);

            String summary = paper.getSummary().isEmpty()
                    ? "No summary added yet."
                    : paper.getSummary();
            txtPaperSummary.setText(summary);

            String status = paper.getStatus();
            String statusText;

            if ("READ".equals(status)) {
                statusText = "Read";
            } else if ("READING".equals(status)) {
                statusText = "Reading";
            } else {
                statusText = "Unread";
            }

            if (paper.isImportant()) {
                statusText += " \u2022 Important";
            }

            txtPaperStatus.setText(statusText);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPaperClick(paper);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onPaperLongClick(paper);
                }
                return true;
            });
        }
    }
}
