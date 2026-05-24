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
import com.example.mpproject.domain.model.Assessment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

// [Adapter] Assessment rows for ModuleDetailFragment.
public class AssessmentAdapter extends ListAdapter<Assessment, AssessmentAdapter.AssessmentViewHolder> {

    public interface OnAssessmentClickListener { void onAssessmentClick(Assessment assessment); }

    private final OnAssessmentClickListener listener;

    private static final SimpleDateFormat DATE_FMT =
            new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

    public AssessmentAdapter(OnAssessmentClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Assessment> DIFF = new DiffUtil.ItemCallback<Assessment>() {
        @Override
        public boolean areItemsTheSame(@NonNull Assessment o, @NonNull Assessment n) {
            return o.getAssessmentId().equals(n.getAssessmentId());
        }
        @Override
        public boolean areContentsTheSame(@NonNull Assessment o, @NonNull Assessment n) {
            return Objects.equals(o.getTitle(), n.getTitle())
                    && Objects.equals(o.getAssessmentType(), n.getAssessmentType())
                    && Double.compare(o.getWeightingPercent(), n.getWeightingPercent()) == 0
                    && Objects.equals(o.getScoreAchieved(), n.getScoreAchieved())
                    && Objects.equals(o.getScoreMaximum(), n.getScoreMaximum())
                    && Objects.equals(o.getScoreMode(), n.getScoreMode())
                    && Objects.equals(o.getDueDate(), n.getDueDate());
        }
    };

    @NonNull
    @Override
    public AssessmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AssessmentViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_assessment, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AssessmentViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class AssessmentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvType;
        private final TextView tvTitle;
        private final TextView tvWeight;
        private final TextView tvScore;
        private final TextView tvDate;

        AssessmentViewHolder(@NonNull View v) {
            super(v);
            tvType   = v.findViewById(R.id.tv_assessment_type);
            tvTitle  = v.findViewById(R.id.tv_assessment_title);
            tvWeight = v.findViewById(R.id.tv_assessment_weight);
            tvScore  = v.findViewById(R.id.tv_assessment_score);
            tvDate   = v.findViewById(R.id.tv_assessment_date);
        }

        void bind(Assessment a, OnAssessmentClickListener listener) {
            String typeText = (a.getAssessmentType() != null && !a.getAssessmentType().isEmpty())
                    ? a.getAssessmentType() : "Other";
            tvType.setText(typeText);
            tvTitle.setText(a.getTitle());
            tvWeight.setText(String.format(Locale.getDefault(), "%.0f%%", a.getWeightingPercent()));

            if (a.isGraded()) {
                Double contrib = a.getContributionPercent();
                double earned   = contrib != null ? contrib : 0.0;
                double weighting = a.getWeightingPercent();
                tvScore.setText(formatScore(earned) + "/" + formatScore(weighting));
            } else {
                tvScore.setText("Not yet graded");
            }

            if (a.getDueDate() != null) {
                tvDate.setText(DATE_FMT.format(new Date(a.getDueDate())));
                tvDate.setVisibility(View.VISIBLE);
            } else {
                tvDate.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onAssessmentClick(a));
        }

        private static String formatScore(double v) {
            return (v == Math.floor(v) && !Double.isInfinite(v))
                    ? String.valueOf((int) v)
                    : String.format(Locale.getDefault(), "%.1f", v);
        }
    }
}
