package com.example.mpproject.domain.repository;

import android.net.Uri;

import androidx.lifecycle.LiveData;

import com.example.mpproject.data.local.entity.ResearchPaperEntity;

import java.util.List;

public interface ResearchPaperRepository {

    interface OperationCallback {
        void onSuccess();
        void onFailure(String error);
    }

    LiveData<List<ResearchPaperEntity>> getAllPapers(String userId);

    LiveData<List<ResearchPaperEntity>> getPapersByStatus(String userId, String status);

    LiveData<List<ResearchPaperEntity>> getPapersByCategory(String userId, String category);

    LiveData<List<ResearchPaperEntity>> getImportantPapers(String userId);

    LiveData<List<ResearchPaperEntity>> searchPapers(String userId, String query);

    void uploadResearchPaper(
            String userId,
            Uri fileUri,
            String fileName,
            String title,
            String authors,
            String year,
            String category,
            String summary,
            String keyFindings,
            String methodology,
            String relevance,
            String status,
            boolean important,
            OperationCallback callback
    );

    void updatePaper(
            ResearchPaperEntity paper,
            OperationCallback callback
    );

    void deletePaper(
            ResearchPaperEntity paper,
            OperationCallback callback
    );
}