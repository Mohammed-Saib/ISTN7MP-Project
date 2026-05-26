package com.example.mpproject.data.repository;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;

import com.example.mpproject.data.local.dao.ResearchPaperDao;
import com.example.mpproject.data.local.entity.ResearchPaperEntity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.example.mpproject.domain.repository.ResearchPaperRepository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResearchPaperRepositoryImpl implements ResearchPaperRepository {

    private final Context context;
    private final ResearchPaperDao researchPaperDao;
    private final FirebaseFirestore firestore;
    private final FirebaseStorage storage;
    private final ExecutorService executorService;

    public ResearchPaperRepositoryImpl(Context context, ResearchPaperDao researchPaperDao) {
        this.context = context.getApplicationContext();
        this.researchPaperDao = researchPaperDao;
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
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
        String storagePath = "users/" + userId + "/research_papers/" + paperId + "_" + safeFileName;

        StorageReference storageRef = storage.getReference().child(storagePath);

        storageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri -> {
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
                                            downloadUri.toString(),
                                            storagePath,
                                            cleanStatus(status),
                                            important,
                                            now,
                                            now
                                    );

                                    savePaperToFirestoreAndRoom(userId, paper, callback);
                                })
                                .addOnFailureListener(e ->
                                        callback.onFailure("Upload succeeded, but download URL failed: " + e.getMessage())
                                )
                )
                .addOnFailureListener(e ->
                        callback.onFailure("Upload failed: " + e.getMessage())
                );
    }

    private void savePaperToFirestoreAndRoom(String userId,
                                             ResearchPaperEntity paper,
                                             OperationCallback callback) {
        firestore.collection("users")
                .document(userId)
                .collection("researchPapers")
                .document(paper.getPaperId())
                .set(paper)
                .addOnSuccessListener(unused -> executorService.execute(() -> {
                    researchPaperDao.insert(paper);
                    callback.onSuccess();
                }))
                .addOnFailureListener(e ->
                        callback.onFailure("Failed to save paper details: " + e.getMessage())
                );
    }

    @Override
    public void updatePaper(ResearchPaperEntity paper, OperationCallback callback) {
        if (paper == null) {
            callback.onFailure("Paper is missing.");
            return;
        }

        paper.setUpdatedAt(System.currentTimeMillis());

        firestore.collection("users")
                .document(paper.getUserId())
                .collection("researchPapers")
                .document(paper.getPaperId())
                .set(paper)
                .addOnSuccessListener(unused -> executorService.execute(() -> {
                    researchPaperDao.update(paper);
                    callback.onSuccess();
                }))
                .addOnFailureListener(e ->
                        callback.onFailure("Failed to update paper: " + e.getMessage())
                );
    }

    @Override
    public void deletePaper(ResearchPaperEntity paper, OperationCallback callback) {
        if (paper == null) {
            callback.onFailure("Paper is missing.");
            return;
        }

        StorageReference storageRef = storage.getReference().child(paper.getStoragePath());

        storageRef.delete()
                .addOnSuccessListener(unused -> deletePaperMetadata(paper, callback))
                .addOnFailureListener(e -> {
                    // Still delete metadata even if file delete fails, because URL/file may already be gone.
                    deletePaperMetadata(paper, callback);
                });
    }

    private void deletePaperMetadata(ResearchPaperEntity paper, OperationCallback callback) {
        firestore.collection("users")
                .document(paper.getUserId())
                .collection("researchPapers")
                .document(paper.getPaperId())
                .delete()
                .addOnSuccessListener(unused -> executorService.execute(() -> {
                    researchPaperDao.deleteById(paper.getPaperId(), paper.getUserId());
                    callback.onSuccess();
                }))
                .addOnFailureListener(e ->
                        callback.onFailure("Failed to delete paper details: " + e.getMessage())
                );
    }

    private String makeSafeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "research_paper.pdf";
        }

        return fileName
                .trim()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
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