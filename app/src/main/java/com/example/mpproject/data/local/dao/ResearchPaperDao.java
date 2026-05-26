package com.example.mpproject.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mpproject.data.local.entity.ResearchPaperEntity;

import java.util.List;

@Dao
public interface ResearchPaperDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ResearchPaperEntity paper);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ResearchPaperEntity> papers);

    @Update
    void update(ResearchPaperEntity paper);

    @Delete
    void delete(ResearchPaperEntity paper);

    @Query("SELECT * FROM research_papers WHERE userId = :userId ORDER BY uploadedAt DESC")
    LiveData<List<ResearchPaperEntity>> getAllPapers(String userId);

    @Query("SELECT * FROM research_papers WHERE userId = :userId AND status = :status ORDER BY uploadedAt DESC")
    LiveData<List<ResearchPaperEntity>> getPapersByStatus(String userId, String status);

    @Query("SELECT * FROM research_papers WHERE userId = :userId AND category = :category ORDER BY uploadedAt DESC")
    LiveData<List<ResearchPaperEntity>> getPapersByCategory(String userId, String category);

    @Query("SELECT * FROM research_papers WHERE userId = :userId AND important = 1 ORDER BY uploadedAt DESC")
    LiveData<List<ResearchPaperEntity>> getImportantPapers(String userId);

    @Query("SELECT * FROM research_papers WHERE userId = :userId AND " +
            "(title LIKE '%' || :query || '%' " +
            "OR authors LIKE '%' || :query || '%' " +
            "OR category LIKE '%' || :query || '%' " +
            "OR summary LIKE '%' || :query || '%') " +
            "ORDER BY uploadedAt DESC")
    LiveData<List<ResearchPaperEntity>> searchPapers(String userId, String query);

    @Query("DELETE FROM research_papers WHERE paperId = :paperId AND userId = :userId")
    void deleteById(String paperId, String userId);

    @Query("DELETE FROM research_papers WHERE userId = :userId")
    void deleteAllForUser(String userId);
}