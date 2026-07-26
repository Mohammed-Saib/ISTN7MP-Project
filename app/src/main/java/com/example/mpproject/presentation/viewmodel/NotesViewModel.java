package com.example.mpproject.presentation.viewmodel;

import android.os.Handler;
import android.os.Looper;
import android.text.Html;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mpproject.domain.model.Module;
import com.example.mpproject.domain.model.ModuleNote;
import com.example.mpproject.domain.model.NoteFolder;
import com.example.mpproject.domain.model.PersonalNote;
import com.example.mpproject.domain.model.PersonalNoteAttachment;
import com.example.mpproject.domain.repository.ModuleNoteRepository;
import com.example.mpproject.domain.repository.ModuleRepository;
import com.example.mpproject.domain.repository.NoteFolderRepository;
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
    private final NoteFolderRepository noteFolderRepository;

    private final LiveData<List<Module>> modules;
    private final LiveData<List<ModuleNote>> moduleNotes;
    private final LiveData<List<PersonalNote>> personalNotes;
    private final LiveData<List<NoteFolder>> folders;

    private final MediatorLiveData<List<NoteListItem>> displayNotes = new MediatorLiveData<>();

    private String searchQuery = "";
    private String selectedModuleId = null;
    private String selectedFolderId = null;
    private int currentFilter = FILTER_ALL;

    private List<Module> latestModules = new ArrayList<>();
    private List<ModuleNote> latestModuleNotes = new ArrayList<>();
    private List<PersonalNote> latestPersonalNotes = new ArrayList<>();
    private List<NoteFolder> latestFolders = new ArrayList<>();

    public NotesViewModel(
            String userId,
            ModuleRepository moduleRepository,
            ModuleNoteRepository moduleNoteRepository,
            PersonalNoteRepository personalNoteRepository,
            PersonalNoteAttachmentRepository personalNoteAttachmentRepository,
            NoteFolderRepository noteFolderRepository
    ) {
        this.userId = userId;
        this.moduleRepository = moduleRepository;
        this.moduleNoteRepository = moduleNoteRepository;
        this.personalNoteRepository = personalNoteRepository;
        this.personalNoteAttachmentRepository = personalNoteAttachmentRepository;
        this.noteFolderRepository = noteFolderRepository;

        personalNoteRepository.syncFromFirestore(userId);
        personalNoteAttachmentRepository.syncFromFirestore(userId);
        noteFolderRepository.syncFromFirestore(userId);

        modules = moduleRepository.getActiveByUser(userId);
        moduleNotes = moduleNoteRepository.getAllByUser(userId);
        personalNotes = personalNoteRepository.getAllByUser(userId);
        folders = noteFolderRepository.getAllByUser(userId);

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

        displayNotes.addSource(folders, list -> {
            latestFolders = list != null ? list : new ArrayList<>();
            rebuildList();
        });
    }

    public LiveData<List<NoteListItem>> getDisplayNotes() {
        return displayNotes;
    }

    public LiveData<List<Module>> getModules() {
        return modules;
    }

    public LiveData<List<NoteFolder>> getFolders() {
        return folders;
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
        selectedModuleId = normalizeNullableId(moduleId);
        rebuildList();
    }

    public void setSelectedFolderId(String folderId) {
        selectedFolderId = normalizeNullableId(folderId);
        rebuildList();
    }

    public String getSelectedModuleIdValue() {
        return selectedModuleId;
    }

    public String getSelectedFolderIdValue() {
        return selectedFolderId;
    }

    public NoteFolder addFolder(String folderName) {
        if (folderName == null || folderName.trim().isEmpty()) return null;

        long now = System.currentTimeMillis();

        NoteFolder folder = new NoteFolder(
                UUID.randomUUID().toString(),
                userId,
                folderName.trim()
        );

        folder.setCreatedAt(now);
        folder.setUpdatedAt(now);

        noteFolderRepository.insert(folder);

        latestFolders.add(folder);
        rebuildList();

        return folder;
    }

    public void renameFolder(NoteFolder folder, String newName) {
        if (folder == null || newName == null || newName.trim().isEmpty()) return;

        folder.setName(newName.trim());
        folder.setUpdatedAt(System.currentTimeMillis());

        noteFolderRepository.update(folder);

        for (int i = 0; i < latestFolders.size(); i++) {
            NoteFolder existing = latestFolders.get(i);
            if (existing.getFolderId() != null && existing.getFolderId().equals(folder.getFolderId())) {
                latestFolders.set(i, folder);
                break;
            }
        }

        rebuildList();
    }

    public void deleteFolder(NoteFolder folder) {
        if (folder == null) return;

        String folderId = folder.getFolderId();
        if (folderId == null || folderId.trim().isEmpty()) return;

        for (PersonalNote note : latestPersonalNotes) {
            if (folderId.equals(note.getFolderId())) {
                note.setFolderId(null);
                note.setUpdatedAt(System.currentTimeMillis());
                personalNoteRepository.update(note, null);
            }
        }

        for (ModuleNote note : latestModuleNotes) {
            if (folderId.equals(note.getFolderId())) {
                note.setFolderId(null);
                moduleNoteRepository.update(note);
            }
        }

        if (folderId.equals(selectedFolderId)) {
            selectedFolderId = null;
        }

        noteFolderRepository.delete(folder);

        List<NoteFolder> updatedFolders = new ArrayList<>();
        for (NoteFolder existing : latestFolders) {
            if (existing.getFolderId() == null || !existing.getFolderId().equals(folderId)) {
                updatedFolders.add(existing);
            }
        }
        latestFolders = updatedFolders;

        rebuildList();
    }

    public void moveNoteToFolder(NoteListItem item, String folderIdOrNull) {
        if (item == null) return;

        String cleanFolderId = normalizeNullableId(folderIdOrNull);

        if (item.getType() == NoteListItem.TYPE_PERSONAL_NOTE) {
            PersonalNote note = findPersonalNote(item.getId());
            if (note == null) return;

            note.setFolderId(cleanFolderId);
            note.setUpdatedAt(System.currentTimeMillis());

            personalNoteRepository.update(note, success -> {
                if (success) {
                    for (int i = 0; i < latestPersonalNotes.size(); i++) {
                        PersonalNote existing = latestPersonalNotes.get(i);
                        if (existing.getNoteId() != null && existing.getNoteId().equals(note.getNoteId())) {
                            latestPersonalNotes.set(i, note);
                            break;
                        }
                    }
                    rebuildList();
                }
            });

        } else {
            ModuleNote note = findModuleNote(item.getId());
            if (note == null) return;

            note.setFolderId(cleanFolderId);

            moduleNoteRepository.update(note);

            for (int i = 0; i < latestModuleNotes.size(); i++) {
                ModuleNote existing = latestModuleNotes.get(i);
                if (existing.getNoteId() != null && existing.getNoteId().equals(note.getNoteId())) {
                    latestModuleNotes.set(i, note);
                    break;
                }
            }
        }
    }

    public ModuleNote createUploadingModuleNote(String moduleId,
                                                String title,
                                                String fileName,
                                                String fileType,
                                                long fileSizeBytes) {
        return createUploadingModuleNote(moduleId, title, fileName, fileType, fileSizeBytes, null);
    }

    public ModuleNote createUploadingModuleNote(String moduleId,
                                                String title,
                                                String fileName,
                                                String fileType,
                                                long fileSizeBytes,
                                                String folderIdOrNull) {
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
        note.setFolderId(normalizeNullableId(folderIdOrNull));

        moduleNoteRepository.insert(note);

        latestModuleNotes.add(note);
        rebuildList();

        return note;
    }

    public void markModuleNoteUploaded(ModuleNote note, String downloadUrl) {
        if (note == null) return;

        note.setStorageUri(downloadUrl);
        note.setUploadStatus("DONE");

        moduleNoteRepository.update(note);

        for (int i = 0; i < latestModuleNotes.size(); i++) {
            ModuleNote existing = latestModuleNotes.get(i);
            if (existing.getNoteId() != null && existing.getNoteId().equals(note.getNoteId())) {
                latestModuleNotes.set(i, note);
                break;
            }
        }

        rebuildList();
    }

    public void markModuleNoteFailed(ModuleNote note) {
        if (note == null) return;

        note.setUploadStatus("FAILED");

        moduleNoteRepository.update(note);

        for (int i = 0; i < latestModuleNotes.size(); i++) {
            ModuleNote existing = latestModuleNotes.get(i);
            if (existing.getNoteId() != null && existing.getNoteId().equals(note.getNoteId())) {
                latestModuleNotes.set(i, note);
                break;
            }
        }

        rebuildList();
    }

    public void addPersonalNote(String title,
                                String content,
                                String moduleIdOrNull,
                                String folderIdOrNull,
                                PersonalNoteCallback callback) {
        long now = System.currentTimeMillis();

        PersonalNote note = new PersonalNote(
                UUID.randomUUID().toString(),
                userId,
                title
        );

        note.setContent(content);
        note.setModuleId(normalizeNullableId(moduleIdOrNull));
        note.setFolderId(normalizeNullableId(folderIdOrNull));
        note.setPinned(false);
        note.setShared(false);
        note.setShareCode(null);
        note.setCreatedAt(now);
        note.setUpdatedAt(now);

        personalNoteRepository.insert(note, success -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (success) {
                    if (callback != null) callback.onSuccess(note);
                } else {
                    if (callback != null) callback.onError("Failed to save note");
                }
            });
        });
    }

    public void updatePersonalNote(NoteListItem item,
                                   String newTitle,
                                   String newHtmlContent,
                                   String moduleIdOrNull,
                                   String folderIdOrNull,
                                   PersonalNoteCallback callback) {
        if (item == null || item.getType() != NoteListItem.TYPE_PERSONAL_NOTE) {
            if (callback != null) callback.onError("Invalid note");
            return;
        }

        PersonalNote note = findPersonalNote(item.getId());
        if (note == null) {
            if (callback != null) callback.onError("Note not found");
            return;
        }

        note.setTitle(newTitle);
        note.setContent(newHtmlContent);
        note.setModuleId(normalizeNullableId(moduleIdOrNull));
        note.setFolderId(normalizeNullableId(folderIdOrNull));
        note.setUpdatedAt(System.currentTimeMillis());

        personalNoteRepository.update(note, success -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (success) {
                    if (callback != null) callback.onSuccess(note);
                } else {
                    if (callback != null) callback.onError("Failed to update note");
                }
            });
        });
    }

    public void createUploadingPersonalAttachment(
            String noteId,
            String fileName,
            String fileType,
            long fileSizeBytes,
            PersonalNoteAttachmentCallback callback
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

        personalNoteAttachmentRepository.insert(attachment, success -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (success) {
                    if (callback != null) callback.onSuccess(attachment);
                } else {
                    if (callback != null) callback.onError("Failed to save attachment");
                }
            });
        });
    }

    public void markPersonalAttachmentUploaded(
            PersonalNoteAttachment attachment,
            String storagePath,
            String downloadUrl,
            PersonalNoteAttachmentCallback callback
    ) {
        if (attachment == null) {
            if (callback != null) callback.onError("Attachment is null");
            return;
        }

        attachment.setStoragePath(storagePath);
        attachment.setDownloadUrl(downloadUrl);
        attachment.setUploadStatus("DONE");

        personalNoteAttachmentRepository.update(attachment, success -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                if (callback != null) {
                    if (success) callback.onSuccess(attachment);
                    else callback.onError("Failed to finalize attachment");
                }
            });
        });
    }

    public void markPersonalAttachmentFailed(PersonalNoteAttachment attachment) {
        if (attachment == null) return;

        attachment.setUploadStatus("FAILED");

        personalNoteAttachmentRepository.update(attachment, null);
    }

    public void deletePersonalAttachment(PersonalNoteAttachment attachment) {
        if (attachment == null) return;

        personalNoteAttachmentRepository.delete(attachment);
    }

    public void renameNote(NoteListItem item, String newTitle) {
        if (item == null || newTitle == null || newTitle.trim().isEmpty()) return;

        if (item.getType() == NoteListItem.TYPE_PERSONAL_NOTE) {
            PersonalNote note = findPersonalNote(item.getId());
            if (note == null) return;

            note.setTitle(newTitle.trim());
            note.setUpdatedAt(System.currentTimeMillis());

            personalNoteRepository.update(note, success -> {
                if (success) {
                    for (int i = 0; i < latestPersonalNotes.size(); i++) {
                        PersonalNote existing = latestPersonalNotes.get(i);
                        if (existing.getNoteId() != null && existing.getNoteId().equals(note.getNoteId())) {
                            latestPersonalNotes.set(i, note);
                            break;
                        }
                    }
                    rebuildList();
                }
            });

        } else {
            ModuleNote note = findModuleNote(item.getId());
            if (note == null) return;

            note.setTitle(newTitle.trim());

            moduleNoteRepository.update(note);

            for (int i = 0; i < latestModuleNotes.size(); i++) {
                ModuleNote existing = latestModuleNotes.get(i);
                if (existing.getNoteId() != null && existing.getNoteId().equals(note.getNoteId())) {
                    latestModuleNotes.set(i, note);
                    break;
                }
            }

            rebuildList();
        }
    }

    public void deleteNote(NoteListItem item) {
        if (item == null) return;

        if (item.getType() == NoteListItem.TYPE_PERSONAL_NOTE) {
            PersonalNote note = findPersonalNote(item.getId());
            if (note != null) {
                personalNoteRepository.delete(note);

                List<PersonalNote> updated = new ArrayList<>();
                for (PersonalNote existing : latestPersonalNotes) {
                    if (existing.getNoteId() == null || !existing.getNoteId().equals(note.getNoteId())) {
                        updated.add(existing);
                    }
                }
                latestPersonalNotes = updated;
            }

        } else {
            ModuleNote note = findModuleNote(item.getId());
            if (note != null) {
                moduleNoteRepository.delete(note);

                List<ModuleNote> updated = new ArrayList<>();
                for (ModuleNote existing : latestModuleNotes) {
                    if (existing.getNoteId() == null || !existing.getNoteId().equals(note.getNoteId())) {
                        updated.add(existing);
                    }
                }
                latestModuleNotes = updated;
            }
        }

        rebuildList();
    }

    private void rebuildList() {
        List<NoteListItem> combined = new ArrayList<>();

        if (currentFilter == FILTER_ALL || currentFilter == FILTER_MODULE) {
            for (ModuleNote note : latestModuleNotes) {
                if (!matchesSelectedModule(note.getModuleId())) continue;
                if (!matchesSelectedFolder(note.getFolderId())) continue;

                String moduleName = getModuleName(note.getModuleId());
                String folderName = getFolderName(note.getFolderId());

                String subtitle = moduleName + " \u2022 " + readableStatus(note.getUploadStatus());

                if (folderName != null && !folderName.trim().isEmpty()) {
                    subtitle = subtitle + " \u2022 \uD83D\uDCC1 " + folderName;
                }

                NoteListItem item = new NoteListItem(
                        NoteListItem.TYPE_MODULE_FILE,
                        note.getNoteId(),
                        note.getTitle(),
                        subtitle,
                        note.getFileName(),
                        note.getStorageUri(),
                        note.getFolderId(),
                        note.getModuleId()
                );

                if (matchesSearch(item)) {
                    combined.add(item);
                }
            }
        }

        if (currentFilter == FILTER_ALL || currentFilter == FILTER_PERSONAL) {
            for (PersonalNote note : latestPersonalNotes) {
                if (!matchesSelectedModule(note.getModuleId())) continue;
                if (!matchesSelectedFolder(note.getFolderId())) continue;

                String moduleName = note.getModuleId() == null || note.getModuleId().trim().isEmpty()
                        ? "Personal"
                        : getModuleName(note.getModuleId());

                String folderName = getFolderName(note.getFolderId());

                String subtitle = moduleName;

                if (folderName != null && !folderName.trim().isEmpty()) {
                    subtitle = subtitle + " \u2022 \uD83D\uDCC1 " + folderName;
                }

                String content = note.getContent() == null ? "" : note.getContent();

                NoteListItem item = new NoteListItem(
                        NoteListItem.TYPE_PERSONAL_NOTE,
                        note.getNoteId(),
                        note.getTitle(),
                        subtitle,
                        content,
                        null,
                        note.getFolderId(),
                        note.getModuleId()
                );

                if (matchesSearch(item)) {
                    combined.add(item);
                }
            }
        }

        displayNotes.postValue(combined);
    }

    private boolean matchesSelectedModule(String moduleId) {
        if (selectedModuleId == null) return true;
        return moduleId != null && moduleId.equals(selectedModuleId);
    }

    private boolean matchesSelectedFolder(String folderId) {
        if (selectedFolderId == null) return true;
        return folderId != null && folderId.equals(selectedFolderId);
    }

    private boolean matchesSearch(NoteListItem item) {
        if (searchQuery.isEmpty()) return true;

        return safe(item.getTitle()).contains(searchQuery)
                || safe(item.getSubtitle()).contains(searchQuery)
                || safe(stripHtml(item.getContentPreview())).contains(searchQuery);
    }

    private PersonalNote findPersonalNote(String noteId) {
        if (noteId == null) return null;

        for (PersonalNote note : latestPersonalNotes) {
            if (noteId.equals(note.getNoteId())) {
                return note;
            }
        }

        return null;
    }

    private ModuleNote findModuleNote(String noteId) {
        if (noteId == null) return null;

        for (ModuleNote note : latestModuleNotes) {
            if (noteId.equals(note.getNoteId())) {
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

    private String getFolderName(String folderId) {
        if (folderId == null || folderId.trim().isEmpty()) return null;

        for (NoteFolder folder : latestFolders) {
            if (folder.getFolderId() != null && folder.getFolderId().equals(folderId)) {
                return folder.getName();
            }
        }

        return null;
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

    private String normalizeNullableId(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    public interface PersonalNoteCallback {
        void onSuccess(PersonalNote note);
        void onError(String error);
    }

    public interface PersonalNoteAttachmentCallback {
        void onSuccess(PersonalNoteAttachment attachment);
        void onError(String error);
    }
}