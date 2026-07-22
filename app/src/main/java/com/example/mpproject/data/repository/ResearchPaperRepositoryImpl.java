package com.example.mpproject.data.repository;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;

import com.example.mpproject.data.local.dao.ResearchPaperDao;
import com.example.mpproject.data.local.entity.ResearchPaperEntity;
import com.example.mpproject.domain.repository.ResearchPaperRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResearchPaperRepositoryImpl implements ResearchPaperRepository {

    private final Context context;
    private final ResearchPaperDao researchPaperDao;
    private final ExecutorService executorService;

    public ResearchPaperRepositoryImpl(Context context, ResearchPaperDao researchPaperDao) {
        this.context = context.getApplicationContext();
        this.researchPaperDao = researchPaperDao;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public LiveData<List<ResearchPaperEntity>> getAllPapers(String userId) {
        return researchPaperDao.getAllPapers(userId);
    }

    @Override
    public LiveData<List<ResearchPaperEntity>> getPapersByStatus(String userId, String status) {
        return researchPaperDao.getPapersByStatus(userId, status);
    }

    @Override
    public LiveData<List<ResearchPaperEntity>> getPapersByCategory(String userId, String category) {
        return researchPaperDao.getPapersByCategory(userId, category);
    }

    @Override
    public LiveData<List<ResearchPaperEntity>> getImportantPapers(String userId) {
        return researchPaperDao.getImportantPapers(userId);
    }

    @Override
    public LiveData<List<ResearchPaperEntity>> searchPapers(String userId, String query) {
        return researchPaperDao.searchPapers(userId, query);
    }

    @Override
    public void uploadResearchPaper(String userId,
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
                                    OperationCallback callback) {

        if (userId == null || userId.trim().isEmpty()) {
            callback.onFailure("User not logged in.");
            return;
        }

        if (fileUri == null) {
            callback.onFailure("Please select a research paper file.");
            return;
        }

        String paperId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        String safeFileName = makeSafeFileName(fileName);
        String localPath = "research_papers/" + userId + "/" + paperId + "_" + safeFileName;

        executorService.execute(() -> {
            try {
                File localFile = new File(context.getFilesDir(), localPath);
                localFile.getParentFile().mkdirs();

                InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
                if (inputStream == null) {
                    callback.onFailure("Could not read the selected file.");
                    return;
                }

                FileOutputStream outputStream = new FileOutputStream(localFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();

                String absolutePath = localFile.getAbsolutePath();

                ResearchPaperEntity paper = new ResearchPaperEntity(
                        paperId,
                        userId,
                        clean(title),
                        clean(authors),
                        clean(year),
                        clean(category),
                        clean(summary),
                        clean(keyFindings),
                        clean(methodology),
                        clean(relevance),
                        safeFileName,
                        absolutePath,
                        localPath,
                        cleanStatus(status),
                        important,
                        now,
                        now
                );

                researchPaperDao.insert(paper);
                callback.onSuccess();
            } catch (Exception e) {
                String message = e.getMessage() == null
                        ? "Upload failed. Please try again."
                        : "Upload failed: " + e.getMessage();
                callback.onFailure(message);
            }
        });
    }

    @Override
    public void updatePaper(ResearchPaperEntity paper, OperationCallback callback) {
        if (paper == null) {
            callback.onFailure("Paper is missing.");
            return;
        }

        if (paper.getUserId() == null || paper.getUserId().trim().isEmpty()) {
            callback.onFailure("User ID is missing.");
            return;
        }

        if (paper.getPaperId() == null || paper.getPaperId().trim().isEmpty()) {
            callback.onFailure("Paper ID is missing.");
            return;
        }

        paper.setUpdatedAt(System.currentTimeMillis());

        executorService.execute(() -> {
            researchPaperDao.insert(paper);
            callback.onSuccess();
        });
    }

    @Override
    public void deletePaper(ResearchPaperEntity paper, OperationCallback callback) {
        if (paper == null) {
            callback.onFailure("Paper is missing.");
            return;
        }

        if (paper.getUserId() == null || paper.getUserId().trim().isEmpty()) {
            callback.onFailure("User ID is missing.");
            return;
        }

        if (paper.getPaperId() == null || paper.getPaperId().trim().isEmpty()) {
            callback.onFailure("Paper ID is missing.");
            return;
        }

        executorService.execute(() -> {
            // Delete local file if it exists
            String localPath = paper.getStoragePath();
            if (localPath != null && !localPath.trim().isEmpty()) {
                File localFile = new File(context.getFilesDir(), localPath);
                if (localFile.exists()) {
                    localFile.delete();
                }
            }

            researchPaperDao.deleteById(paper.getPaperId(), paper.getUserId());
            callback.onSuccess();
        });
    }

    private String makeSafeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "research_paper.pdf";
        }
        return fileName.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanStatus(String status) {
        if (status == null) return "UNREAD";
        String cleaned = status.trim().toUpperCase();
        if (cleaned.equals("READING")) return "READING";
        if (cleaned.equals("READ")) return "READ";
        return "UNREAD";
    }
}
