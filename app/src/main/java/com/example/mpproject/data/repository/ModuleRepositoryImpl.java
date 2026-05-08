package com.example.mpproject.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.dao.ModuleDao;
import com.example.mpproject.data.local.entity.ModuleEntity;
import com.example.mpproject.domain.model.Module;
import com.example.mpproject.domain.repository.ModuleRepository;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// [Data] Implements ModuleRepository. Room is the source of truth; Firestore is synced in the background.
public class ModuleRepositoryImpl implements ModuleRepository {

    private static final String TAG = "ModuleRepository";

    private final ModuleDao moduleDao;
    private final FirebaseFirestore firestore;

    public ModuleRepositoryImpl(ModuleDao moduleDao) {
        this.moduleDao = moduleDao;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public void insert(Module module) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            moduleDao.insert(toEntity(module));
            syncToFirestore(module);
        });
    }

    @Override
    public void update(Module module) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            moduleDao.update(toEntity(module));
            syncToFirestore(module);
        });
    }

    @Override
    public void delete(Module module) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            moduleDao.delete(toEntity(module));
            // Remove from Firestore; ignore failures (can be retried on next sync)
            try {
                firestore.collection("users").document(module.getUserId())
                        .collection("modules").document(module.getModuleId())
                        .delete()
                        .addOnFailureListener(e -> Log.e(TAG, "Firestore delete failed", e));
            } catch (Exception e) {
                Log.e(TAG, "Firestore delete error", e);
            }
        });
    }

    @Override
    public LiveData<Module> getById(String moduleId) {
        return Transformations.map(moduleDao.getById(moduleId), this::toDomain);
    }

    @Override
    public LiveData<List<Module>> getAllByUser(String userId) {
        return Transformations.map(moduleDao.getAllByUser(userId), this::toDomainList);
    }

    @Override
    public LiveData<List<Module>> getActiveByUser(String userId) {
        return Transformations.map(moduleDao.getActiveByUser(userId), this::toDomainList);
    }

    @Override
    public LiveData<List<Module>> getArchivedByUser(String userId) {
        return Transformations.map(moduleDao.getArchivedByUser(userId), this::toDomainList);
    }

    // Push module data to users/{userId}/modules/{moduleId} in Firestore
    private void syncToFirestore(Module module) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("name", module.getName());
            data.put("moduleCode", module.getModuleCode());
            data.put("lecturerName", module.getLecturerName());
            data.put("lecturerEmail", module.getLecturerEmail());
            data.put("lecturerOfficeHours", module.getLecturerOfficeHours());
            data.put("color", module.getColor());
            data.put("semester", module.getSemester());
            data.put("isArchived", module.isArchived());
            data.put("createdAt", module.getCreatedAt());
            data.put("updatedAt", module.getUpdatedAt());

            firestore.collection("users").document(module.getUserId())
                    .collection("modules").document(module.getModuleId())
                    .set(data)
                    .addOnFailureListener(e -> Log.e(TAG, "Firestore sync failed", e));
        } catch (Exception e) {
            Log.e(TAG, "Firestore sync error", e);
        }
    }

    private Module toDomain(ModuleEntity e) {
        if (e == null) return null;
        Module m = new Module(e.getModuleId(), e.getUserId(), e.getName());
        m.setModuleCode(e.getModuleCode());
        m.setLecturerName(e.getLecturerName());
        m.setLecturerEmail(e.getLecturerEmail());
        m.setLecturerOfficeHours(e.getLecturerOfficeHours());
        m.setColor(e.getColor());
        m.setSemester(e.getSemester());
        m.setArchived(e.isArchived());
        m.setUpdatedAt(e.getUpdatedAt());
        return m;
    }

    private List<Module> toDomainList(List<ModuleEntity> entities) {
        List<Module> list = new ArrayList<>();
        if (entities != null) {
            for (ModuleEntity e : entities) list.add(toDomain(e));
        }
        return list;
    }

    private ModuleEntity toEntity(Module m) {
        ModuleEntity e = new ModuleEntity();
        e.setModuleId(m.getModuleId());
        e.setUserId(m.getUserId());
        e.setName(m.getName());
        e.setModuleCode(m.getModuleCode());
        e.setLecturerName(m.getLecturerName());
        e.setLecturerEmail(m.getLecturerEmail());
        e.setLecturerOfficeHours(m.getLecturerOfficeHours());
        e.setColor(m.getColor());
        e.setSemester(m.getSemester());
        e.setArchived(m.isArchived());
        e.setCreatedAt(m.getCreatedAt());
        e.setUpdatedAt(m.getUpdatedAt());
        return e;
    }
}
