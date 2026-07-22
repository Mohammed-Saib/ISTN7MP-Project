package com.example.mpproject.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.dao.ModuleDao;
import com.example.mpproject.data.local.entity.ModuleEntity;
import com.example.mpproject.domain.model.Module;
import com.example.mpproject.domain.repository.ModuleRepository;

import java.util.ArrayList;
import java.util.List;

public class ModuleRepositoryImpl implements ModuleRepository {

    private final ModuleDao moduleDao;

    public ModuleRepositoryImpl(ModuleDao moduleDao) {
        this.moduleDao = moduleDao;
    }

    @Override
    public void insert(Module module) {
        AppDatabase.databaseWriteExecutor.execute(() -> moduleDao.insert(toEntity(module)));
    }

    @Override
    public void update(Module module) {
        AppDatabase.databaseWriteExecutor.execute(() -> moduleDao.update(toEntity(module)));
    }

    @Override
    public void delete(Module module) {
        AppDatabase.databaseWriteExecutor.execute(() -> moduleDao.delete(toEntity(module)));
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
        m.setCreatedAt(e.getCreatedAt());
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
