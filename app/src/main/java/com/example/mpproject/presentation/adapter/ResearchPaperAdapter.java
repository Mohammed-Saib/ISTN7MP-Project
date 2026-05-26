package com.example.mpproject.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mpproject.R;
import com.example.mpproject.data.local.entity.ResearchPaperEntity;

import java.util.ArrayList;
import java.util.List;

public class ResearchPaperAdapter extends RecyclerView.Adapter<ResearchPaperAdapter.ResearchPaperViewHolder> {

    public interface OnResearchPaperClickListener {
        void onPaperClick(ResearchPaperEntity paper);
        void onPaperLongClick(ResearchPaperEntity paper);
    }

    private final List<ResearchPaperEntity> papers = new ArrayList<>();
    private final OnResearchPaperClickListener listener;

    public ResearchPaperAdapter(OnResearchPaperClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ResearchPaperEntity> newPapers) {
        papers.clear();

        if (newPapers != null) {
            papers.addAll(newPapers);
        }

        notifyDataSetChanged();
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
        ResearchPaperEntity paper = papers.get(position);
        holder.bind(paper);
    }

    @Override
    public int getItemCount() {
        return papers.size();
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
            txtPaperMeta.setText(authors + " • " + year);

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
                statusText += " • Important";
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