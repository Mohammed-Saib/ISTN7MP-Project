package com.example.mpproject.data.repository;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;

import com.example.mpproject.data.local.dao.ResearchPaperDao;
import com.example.mpproject.data.local.entity.ResearchPaperEntity;
import com.example.mpproject.domain.repository.ResearchPaperRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
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

    private ListenerRegistration researchPapersListener;
    private String syncedUserId;

    public ResearchPaperRepositoryImpl(Context context, ResearchPaperDao researchPaperDao) {
        this.context = context.getApplicationContext();
        this.researchPaperDao = researchPaperDao;
        this.firestore = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public LiveData<List<ResearchPaperEntity>> getAllPapers(String userId) {
        startResearchPapersSync(userId);
        return researchPaperDao.getAllPapers(userId);
    }

    @Override
    public LiveData<List<ResearchPaperEntity>> getPapersByStatus(String userId, String status) {
        startResearchPapersSync(userId);
        return researchPaperDao.getPapersByStatus(userId, status);
    }

    @Override
    public LiveData<List<ResearchPaperEntity>> getPapersByCategory(String userId, String category) {
        startResearchPapersSync(userId);
        return researchPaperDao.getPapersByCategory(userId, category);
    }

    @Override
    public LiveData<List<ResearchPaperEntity>> getImportantPapers(String userId) {
        startResearchPapersSync(userId);
        return researchPaperDao.getImportantPapers(userId);
    }

    @Override
    public LiveData<List<ResearchPaperEntity>> searchPapers(String userId, String query) {
        startResearchPapersSync(userId);
        return researchPaperDao.searchPapers(userId, query);
    }

    private void startResearchPapersSync(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        if (researchPapersListener != null && userId.equals(syncedUserId)) {
            return;
        }

        if (researchPapersListener != null) {
            researchPapersListener.remove();
            researchPapersListener = null;
        }

        syncedUserId = userId;

        researchPapersListener = firestore.collection("users")
                .document(userId)
                .collection("researchPapers")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        return;
                    }

                    List<ResearchPaperEntity> cloudPapers = new ArrayList<>();

                    for (DocumentSnapshot document : snapshots.getDocuments()) {
                        ResearchPaperEntity paper = document.toObject(ResearchPaperEntity.class);

                        if (paper == null) {
                            continue;
                        }

                        if (paper.getPaperId() == null || paper.getPaperId().trim().isEmpty()) {
                            paper.setPaperId(document.getId());
                        }

                        if (paper.getUserId() == null || paper.getUserId().trim().isEmpty()) {
                            paper.setUserId(userId);
                        }

                        cloudPapers.add(paper);
                    }

                    executorService.execute(() -> {
                        researchPaperDao.deleteAllForUser(userId);
                        researchPaperDao.insertAll(cloudPapers);
                    });
                });
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

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(getContentTypeFromFileName(safeFileName))
                .build();

        storageRef.putFile(fileUri, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception exception = task.getException();

                        if (exception != null) {
                            throw exception;
                        }

                        throw new Exception("Upload failed for an unknown reason.");
                    }

                    return storageRef.getDownloadUrl();
                })
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
                .addOnFailureListener(e -> {
                    safelyDeleteUploadedFile(storageRef);

                    String message = e.getMessage() == null
                            ? "Upload failed. Please try again."
                            : "Upload failed: " + e.getMessage();

                    callback.onFailure(message);
                });
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
                .addOnFailureListener(e -> {
                    if (paper.getStoragePath() != null && !paper.getStoragePath().trim().isEmpty()) {
                        safelyDeleteUploadedFile(storage.getReference().child(paper.getStoragePath()));
                    }

                    String message = e.getMessage() == null
                            ? "Failed to save paper details."
                            : "Failed to save paper details: " + e.getMessage();

                    callback.onFailure(message);
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

        firestore.collection("users")
                .document(paper.getUserId())
                .collection("researchPapers")
                .document(paper.getPaperId())
                .set(paper)
                .addOnSuccessListener(unused -> executorService.execute(() -> {
                    researchPaperDao.insert(paper);
                    callback.onSuccess();
                }))
                .addOnFailureListener(e -> {
                    String message = e.getMessage() == null
                            ? "Failed to update paper."
                            : "Failed to update paper: " + e.getMessage();

                    callback.onFailure(message);
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

        String storagePath = paper.getStoragePath();

        if (storagePath == null || storagePath.trim().isEmpty()) {
            deletePaperMetadata(paper, callback);
            return;
        }

        StorageReference storageRef = storage.getReference().child(storagePath);

        storageRef.delete()
                .addOnSuccessListener(unused -> deletePaperMetadata(paper, callback))
                .addOnFailureListener(e -> {
                    // Still delete metadata even if file delete fails, because the file may already be gone.
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
                .addOnFailureListener(e -> {
                    String message = e.getMessage() == null
                            ? "Failed to delete paper details."
                            : "Failed to delete paper details: " + e.getMessage();

                    callback.onFailure(message);
                });
    }

    private void safelyDeleteUploadedFile(StorageReference ref) {
        if (ref == null) return;

        try {
            ref.delete().addOnFailureListener(e -> {
                // Ignore cleanup failure.
            });
        } catch (Exception ignored) {
            // Ignore cleanup failure.
        }
    }

    private String makeSafeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "research_paper.pdf";
        }

        return fileName
                .trim()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getContentTypeFromFileName(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }

        String lower = fileName.toLowerCase();

        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }

        if (lower.endsWith(".doc")) {
            return "application/msword";
        }

        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }

        if (lower.endsWith(".ppt")) {
            return "application/vnd.ms-powerpoint";
        }

        if (lower.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        }

        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }

        if (lower.endsWith(".png")) {
            return "image/png";
        }

        if (lower.endsWith(".webp")) {
            return "image/webp";
        }

        return "application/octet-stream";
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