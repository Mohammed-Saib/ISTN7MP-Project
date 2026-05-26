package com.example.mpproject.presentation.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mpproject.data.local.entity.ResearchPaperEntity;
import com.example.mpproject.domain.repository.ResearchPaperRepository;

import java.util.List;

public class ResearchViewModel extends ViewModel {

    private final ResearchPaperRepository repository;

    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public ResearchViewModel(ResearchPaperRepository repository) {
        this.repository = repository;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<List<ResearchPaperEntity>> getAllPapers(String userId) {
        return repository.getAllPapers(userId);
    }

    public LiveData<List<ResearchPaperEntity>> getPapersByStatus(String userId, String status) {
        return repository.getPapersByStatus(userId, status);
    }

    public LiveData<List<ResearchPaperEntity>> getPapersByCategory(String userId, String category) {
        return repository.getPapersByCategory(userId, category);
    }

    public LiveData<List<ResearchPaperEntity>> getImportantPapers(String userId) {
        return repository.getImportantPapers(userId);
    }

    public LiveData<List<ResearchPaperEntity>> searchPapers(String userId, String query) {
        return repository.searchPapers(userId, query);
    }

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
                                    boolean important) {

        loading.setValue(true);

        repository.uploadResearchPaper(
                userId,
                fileUri,
                fileName,
                title,
                authors,
                year,
                category,
                summary,
                keyFindings,
                methodology,
                relevance,
                status,
                important,
                new ResearchPaperRepository.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        loading.postValue(false);
                        message.postValue("Research paper saved.");
                    }

                    @Override
                    public void onFailure(String error) {
                        loading.postValue(false);
                        message.postValue(error);
                    }
                }
        );
    }

    public void updatePaper(ResearchPaperEntity paper) {
        loading.setValue(true);

        repository.updatePaper(paper, new ResearchPaperRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                loading.postValue(false);
                message.postValue("Paper updated.");
            }

            @Override
            public void onFailure(String error) {
                loading.postValue(false);
                message.postValue(error);
            }
        });
    }

    public void deletePaper(ResearchPaperEntity paper) {
        loading.setValue(true);

        repository.deletePaper(paper, new ResearchPaperRepository.OperationCallback() {
            @Override
            public void onSuccess() {
                loading.postValue(false);
                message.postValue("Paper deleted.");
            }

            @Override
            public void onFailure(String error) {
                loading.postValue(false);
                message.postValue(error);
            }
        });
    }
}