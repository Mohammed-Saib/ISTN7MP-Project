package com.example.mpproject.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mpproject.domain.model.Module;
import com.example.mpproject.domain.repository.ModuleRepository;

import java.util.List;
import java.util.UUID;

// [ViewModel] Module list — active and archived LiveData; editingModule drives the bottom sheet.
public class ModuleViewModel extends ViewModel {

    private final ModuleRepository moduleRepository;
    private final String userId;

    public final LiveData<List<Module>> activeModules;
    public final LiveData<List<Module>> archivedModules;

    private final MutableLiveData<Module> editingModule = new MutableLiveData<>(null);

    public ModuleViewModel(ModuleRepository moduleRepository, String userId) {
        this.moduleRepository = moduleRepository;
        this.userId = userId;
        activeModules   = moduleRepository.getActiveByUser(userId);
        archivedModules = moduleRepository.getArchivedByUser(userId);
    }

    public LiveData<Module> getEditingModule() { return editingModule; }
    public void setEditingModule(Module module) { editingModule.setValue(module); }

    public void addModule(String name, String code, String color, String semester,
                          String lecturerName, String lecturerEmail, String officeHours) {
        Module m = new Module(UUID.randomUUID().toString(), userId, name);
        m.setModuleCode(code);
        m.setColor(color);
        m.setSemester(semester);
        m.setLecturerName(lecturerName);
        m.setLecturerEmail(lecturerEmail);
        m.setLecturerOfficeHours(officeHours);
        moduleRepository.insert(m);
    }

    public void updateModule(Module module) {
        module.setUpdatedAt(System.currentTimeMillis());
        moduleRepository.update(module);
    }

    public void archiveModule(Module module) {
        module.setArchived(true);
        module.setUpdatedAt(System.currentTimeMillis());
        moduleRepository.update(module);
    }

    public void unarchiveModule(Module module) {
        module.setArchived(false);
        module.setUpdatedAt(System.currentTimeMillis());
        moduleRepository.update(module);
    }

    public void deleteModule(Module module) {
        moduleRepository.delete(module);
    }
}
