
package com.example.mpproject.presentation.viewmodel;

import android.text.Html;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mpproject.domain.model.Module;
import com.example.mpproject.domain.model.ModuleNote;
import com.example.mpproject.domain.model.PersonalNote;
import com.example.mpproject.domain.model.PersonalNoteAttachment;
import com.example.mpproject.domain.repository.ModuleNoteRepository;
import com.example.mpproject.domain.repository.ModuleRepository;
import com.example.mpproject.domain.repository.PersonalNoteAttachmentRepository;
import com.example.mpproject.domain.repository.PersonalNoteRepository;
import com.example.mpproject.presentation.model.NoteListItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotesViewModel extends ViewModel {

    public static final int FILTER_ALL = 0;
    public static final int FILTER_MODULE = 1;
    public static final int FILTER_PERSONAL = 2;

    private final String userId;
    private final ModuleRepository moduleRepository;
    private final ModuleNoteRepository moduleNoteRepository;
    private final PersonalNoteRepository personalNoteRepository;
    private final PersonalNoteAttachmentRepository personalNoteAttachmentRepository;

    private final LiveData<List<Module>> modules;
    private final LiveData<List<ModuleNote>> moduleNotes;
    private final LiveData<List<PersonalNote>> personalNotes;

    private final MediatorLiveData<List<NoteListItem>> displayNotes = new MediatorLiveData<>();

    private String searchQuery = "";
    private String selectedModuleId = null;
    private int currentFilter = FILTER_ALL;

    private List<Module> latestModules = new ArrayList<>();
    private List<ModuleNote> latestModuleNotes = new ArrayList<>();
    private List<PersonalNote> latestPersonalNotes = new ArrayList<>();

    public NotesViewModel(
            String userId,
            ModuleRepository moduleRepository,
            ModuleNoteRepository moduleNoteRepository,
            PersonalNoteRepository personalNoteRepository,
            PersonalNoteAttachmentRepository personalNoteAttachmentRepository
    ) {
        this.userId = userId;
        this.moduleRepository = moduleRepository;
        this.moduleNoteRepository = moduleNoteRepository;
        this.personalNoteRepository = personalNoteRepository;
        this.personalNoteAttachmentRepository = personalNoteAttachmentRepository;

        // Pull cloud data into Room cache so notes appear across devices.
        personalNoteRepository.syncFromFirestore(userId);
        personalNoteAttachmentRepository.syncFromFirestore(userId);

        modules = moduleRepository.getActiveByUser(userId);
        moduleNotes = moduleNoteRepository.getAllByUser(userId);
        personalNotes = personalNoteRepository.getAllByUser(userId);

        displayNotes.addSource(modules, list -> {
            latestModules = list != null ? list : new ArrayList<>();
            rebuildList();
        });

        displayNotes.addSource(moduleNotes, list -> {
            latestModuleNotes = list != null ? list : new ArrayList<>();
            rebuildList();
        });

        displayNotes.addSource(personalNotes, list -> {
            latestPersonalNotes = list != null ? list : new ArrayList<>();
            rebuildList();
        });
    }

    public LiveData<List<NoteListItem>> getDisplayNotes() {
        return displayNotes;
    }

    public LiveData<List<Module>> getModules() {
        return modules;
    }

    public LiveData<List<PersonalNoteAttachment>> getAttachmentsForNote(String noteId) {
        return personalNoteAttachmentRepository.getByNoteId(noteId);
    }

    public void setFilter(int filter) {
        currentFilter = filter;
        rebuildList();
    }

    public void setSearchQuery(String query) {
        searchQuery = query == null ? "" : query.toLowerCase().trim();
        rebuildList();
    }

    public void setSelectedModuleId(String moduleId) {
        selectedModuleId = (moduleId == null || moduleId.trim().isEmpty()) ? null : moduleId;
        rebuildList();
    }

    public ModuleNote createUploadingModuleNote(String moduleId, String title,
                                                String fileName, String fileType,
                                                long fileSizeBytes) {
        ModuleNote note = new ModuleNote(
                UUID.randomUUID().toString(),
                moduleId,
                userId,
                title,
                fileName,
                fileType
        );

        note.setFileSizeBytes(fileSizeBytes);
        note.setUploadStatus("UPLOADING");

        moduleNoteRepository.insert(note);
        return note;
    }

    public void markModuleNoteUploaded(ModuleNote note, String downloadUrl) {
        note.setStorageUri(downloadUrl);
        note.setUploadStatus("DONE");
        moduleNoteRepository.update(note);
    }

    public void markModuleNoteFailed(ModuleNote note) {
        note.setUploadStatus("FAILED");
        moduleNoteRepository.update(note);
    }

    public PersonalNote addPersonalNote(String title, String content, String moduleIdOrNull) {
        long now = System.currentTimeMillis();

        PersonalNote note = new PersonalNote(
                UUID.randomUUID().toString(),
                userId,
                title
        );

        note.setContent(content);
        note.setModuleId(moduleIdOrNull);
        note.setPinned(false);
        note.setShared(false);
        note.setShareCode(null);
        note.setCreatedAt(now);
        note.setUpdatedAt(now);

        personalNoteRepository.insert(note);

        return note;
    }

    public void updatePersonalNote(NoteListItem item, String newTitle, String newHtmlContent) {
        if (item == null || item.getType() != NoteListItem.TYPE_PERSONAL_NOTE) return;

        for (PersonalNote note : latestPersonalNotes) {
            if (note.getNoteId().equals(item.getId())) {
                note.setTitle(newTitle);
                note.setContent(newHtmlContent);
                note.setUpdatedAt(System.currentTimeMillis());

                personalNoteRepository.update(note);
                return;
            }
        }
    }

    public PersonalNoteAttachment createUploadingPersonalAttachment(
            String noteId,
            String fileName,
            String fileType,
            long fileSizeBytes
    ) {
        PersonalNoteAttachment attachment = new PersonalNoteAttachment(
                UUID.randomUUID().toString(),
                noteId,
                userId,
                fileName,
                fileType
        );

        attachment.setFileSizeBytes(fileSizeBytes);
        attachment.setUploadStatus("UPLOADING");

        personalNoteAttachmentRepository.insert(attachment);

        return attachment;
    }

    public void markPersonalAttachmentUploaded(
            PersonalNoteAttachment attachment,
            String storagePath,
            String downloadUrl
    ) {
        attachment.setStoragePath(storagePath);
        attachment.setDownloadUrl(downloadUrl);
        attachment.setUploadStatus("DONE");

        personalNoteAttachmentRepository.update(attachment);
    }

    public void markPersonalAttachmentFailed(PersonalNoteAttachment attachment) {
        attachment.setUploadStatus("FAILED");
        personalNoteAttachmentRepository.update(attachment);
    }

    public void deletePersonalAttachment(PersonalNoteAttachment attachment) {
        personalNoteAttachmentRepository.delete(attachment);
    }

    public void renameNote(NoteListItem item, String newTitle) {
        if (item == null || newTitle == null || newTitle.trim().isEmpty()) return;

        if (item.getType() == NoteListItem.TYPE_PERSONAL_NOTE) {
            PersonalNote note = findPersonalNote(item.getId());
            if (note == null) return;

            note.setTitle(newTitle.trim());
            note.setUpdatedAt(System.currentTimeMillis());
            personalNoteRepository.update(note);
        } else {
            ModuleNote note = findModuleNote(item.getId());
            if (note == null) return;

            note.setTitle(newTitle.trim());
            moduleNoteRepository.update(note);
        }
    }

    public void deleteNote(NoteListItem item) {
        if (item == null) return;

        if (item.getType() == NoteListItem.TYPE_PERSONAL_NOTE) {
            PersonalNote note = findPersonalNote(item.getId());
            if (note != null) personalNoteRepository.delete(note);
        } else {
            ModuleNote note = findModuleNote(item.getId());
            if (note != null) moduleNoteRepository.delete(note);
        }
    }

    private void rebuildList() {
        List<NoteListItem> combined = new ArrayList<>();

        if (currentFilter == FILTER_ALL || currentFilter == FILTER_MODULE) {
            for (ModuleNote note : latestModuleNotes) {
                if (!matchesSelectedModule(note.getModuleId())) continue;

                String moduleName = getModuleName(note.getModuleId());

                NoteListItem item = new NoteListItem(
                        NoteListItem.TYPE_MODULE_FILE,
                        note.getNoteId(),
                        note.getTitle(),
                        moduleName + " • " + readableStatus(note.getUploadStatus()),
                        note.getFileName(),
                        note.getStorageUri()
                );

                if (matchesSearch(item)) combined.add(item);
            }
        }

        if (currentFilter == FILTER_ALL || currentFilter == FILTER_PERSONAL) {
            for (PersonalNote note : latestPersonalNotes) {
                if (!matchesSelectedModule(note.getModuleId())) continue;

                String moduleName = note.getModuleId() == null || note.getModuleId().trim().isEmpty()
                        ? "Personal"
                        : getModuleName(note.getModuleId());

                String content = note.getContent() == null ? "" : note.getContent();

                NoteListItem item = new NoteListItem(
                        NoteListItem.TYPE_PERSONAL_NOTE,
                        note.getNoteId(),
                        note.getTitle(),
                        moduleName,
                        content,
                        null
                );

                if (matchesSearch(item)) combined.add(item);
            }
        }

        displayNotes.setValue(combined);
    }

    private boolean matchesSelectedModule(String moduleId) {
        if (selectedModuleId == null) return true;
        return moduleId != null && moduleId.equals(selectedModuleId);
    }

    private boolean matchesSearch(NoteListItem item) {
        if (searchQuery.isEmpty()) return true;

        return safe(item.getTitle()).contains(searchQuery)
                || safe(item.getSubtitle()).contains(searchQuery)
                || safe(stripHtml(item.getContentPreview())).contains(searchQuery);
    }

    private PersonalNote findPersonalNote(String noteId) {
        for (PersonalNote note : latestPersonalNotes) {
            if (note.getNoteId() != null && note.getNoteId().equals(noteId)) {
                return note;
            }
        }

        return null;
    }

    private ModuleNote findModuleNote(String noteId) {
        for (ModuleNote note : latestModuleNotes) {
            if (note.getNoteId() != null && note.getNoteId().equals(noteId)) {
                return note;
            }
        }

        return null;
    }

    private String getModuleName(String moduleId) {
        if (moduleId == null) return "Personal";

        for (Module module : latestModules) {
            if (module.getModuleId() != null && module.getModuleId().equals(moduleId)) {
                if (module.getModuleCode() != null && !module.getModuleCode().isEmpty()) {
                    return module.getModuleCode() + " - " + module.getName();
                }
                return module.getName();
            }
        }

        return "Module";
    }

    private String readableStatus(String status) {
        if ("UPLOADING".equals(status)) return "Uploading";
        if ("FAILED".equals(status)) return "Upload failed";
        if ("DONE".equals(status)) return "Uploaded";
        return "Pending";
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String stripHtml(String html) {
        if (html == null) return "";

        return Html.fromHtml(
                html,
                Html.FROM_HTML_MODE_LEGACY
        ).toString();
    }
}
