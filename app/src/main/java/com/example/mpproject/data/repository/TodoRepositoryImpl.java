package com.example.mpproject.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.dao.TodoDao;
import com.example.mpproject.data.local.entity.TodoEntity;
import com.example.mpproject.domain.model.Todo;
import com.example.mpproject.domain.repository.TodoRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// [Data] Implements TodoRepository. Room is the source of truth; Firestore is synced in the background.
public class TodoRepositoryImpl implements TodoRepository {

    private static final String TAG = "TodoRepository";

    private final TodoDao todoDao;
    private final FirebaseFirestore firestore;

    public TodoRepositoryImpl(TodoDao todoDao) {
        this.todoDao = todoDao;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public void insert(Todo todo) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            todoDao.insert(toEntity(todo));
            syncToFirestore(todo);
        });
    }

    @Override
    public void update(Todo todo) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            todoDao.update(toEntity(todo));
            syncToFirestore(todo);
        });
    }

    @Override
    public void delete(Todo todo) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            todoDao.delete(toEntity(todo));
            try {
                firestore.collection("users").document(todo.getUserId())
                        .collection("todos").document(todo.getTodoId())
                        .delete()
                        .addOnFailureListener(e -> Log.e(TAG, "Firestore delete failed", e));
            } catch (Exception e) {
                Log.e(TAG, "Firestore delete error", e);
            }
        });
    }

    @Override
    public LiveData<Todo> getById(String todoId) {
        return Transformations.map(todoDao.getById(todoId), this::toDomain);
    }

    @Override
    public LiveData<List<Todo>> getAllByUser(String userId) {
        return Transformations.map(todoDao.getAllByUser(userId), this::toDomainList);
    }

    @Override
    public LiveData<List<Todo>> getCompletedByUser(String userId) {
        return Transformations.map(todoDao.getCompletedByUser(userId), this::toDomainList);
    }

    @Override
    public LiveData<List<Todo>> getAllByModule(String moduleId) {
        return Transformations.map(todoDao.getAllByModule(moduleId), this::toDomainList);
    }

    @Override
    public LiveData<List<Todo>> getByDueDateRange(String userId, long startMs, long endMs) {
        return Transformations.map(todoDao.getByDueDateRange(userId, startMs, endMs), this::toDomainList);
    }

    @Override
    public void updateGroup(Todo representative) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long now = System.currentTimeMillis();
            todoDao.updateGroupFields(
                    representative.getRecurrenceGroupId(),
                    representative.getTitle(),
                    representative.getDescription(),
                    representative.getPriority(),
                    representative.getModuleId(),
                    now);
            syncGroupToFirestore(representative, now);
        });
    }

    @Override
    public void deleteGroup(String recurrenceGroupId, String userId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            todoDao.deleteByGroupId(recurrenceGroupId);
            try {
                firestore.collection("users").document(userId)
                        .collection("todos")
                        .whereEqualTo("recurrenceGroupId", recurrenceGroupId)
                        .get()
                        .addOnSuccessListener(snap -> {
                            WriteBatch batch = firestore.batch();
                            for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                                batch.delete(doc.getReference());
                            }
                            batch.commit()
                                    .addOnFailureListener(e -> Log.e(TAG, "Firestore group delete failed", e));
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Firestore group delete query failed", e));
            } catch (Exception e) {
                Log.e(TAG, "Firestore group delete error", e);
            }
        });
    }

    // Unconditional merge-sync from Firestore: fetches every todo for this user and upserts into Room.
    public void syncFromFirestore(String userId) {
        firestore.collection("users").document(userId)
                .collection("todos")
                .get()
                .addOnSuccessListener(snap ->
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                                try {
                                    TodoEntity e = new TodoEntity();
                                    e.setTodoId(doc.getId());
                                    e.setUserId(userId);
                                    e.setTitle(doc.getString("title"));
                                    e.setDescription(doc.getString("description"));
                                    e.setModuleId(doc.getString("moduleId"));
                                    e.setPriority(doc.getString("priority"));
                                    e.setDueDate(doc.getLong("dueDate"));
                                    Boolean completed = doc.getBoolean("isCompleted");
                                    e.setCompleted(completed != null && completed);
                                    e.setCompletedAt(doc.getLong("completedAt"));
                                    e.setRecurrencePattern(doc.getString("recurrencePattern"));
                                    e.setRecurrenceGroupId(doc.getString("recurrenceGroupId"));
                                    e.setRecurrenceEndDate(doc.getLong("recurrenceEndDate"));
                                    Long createdAt = doc.getLong("createdAt");
                                    e.setCreatedAt(createdAt != null ? createdAt : 0L);
                                    Long updatedAt = doc.getLong("updatedAt");
                                    e.setUpdatedAt(updatedAt != null ? updatedAt : 0L);
                                    todoDao.insert(e);
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error syncing todo from Firestore", ex);
                                }
                            }
                        }))
                .addOnFailureListener(e -> Log.e(TAG, "Firestore todo sync failed", e));
    }

    private void syncToFirestore(Todo todo) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("userId", todo.getUserId());
            data.put("title", todo.getTitle());
            data.put("description", todo.getDescription());
            data.put("moduleId", todo.getModuleId());
            data.put("priority", todo.getPriority());
            data.put("dueDate", todo.getDueDate());
            data.put("isCompleted", todo.isCompleted());
            data.put("completedAt", todo.getCompletedAt());
            data.put("createdAt", todo.getCreatedAt());
            data.put("updatedAt", todo.getUpdatedAt());

            data.put("recurrencePattern", todo.getRecurrencePattern());
            data.put("recurrenceGroupId", todo.getRecurrenceGroupId());
            data.put("recurrenceEndDate", todo.getRecurrenceEndDate());

            firestore.collection("users").document(todo.getUserId())
                    .collection("todos").document(todo.getTodoId())
                    .set(data)
                    .addOnFailureListener(e -> Log.e(TAG, "Firestore sync failed", e));
        } catch (Exception e) {
            Log.e(TAG, "Firestore sync error", e);
        }
    }

    private void syncGroupToFirestore(Todo representative, long updatedAt) {
        try {
            Map<String, Object> patch = new HashMap<>();
            patch.put("title", representative.getTitle());
            patch.put("description", representative.getDescription());
            patch.put("priority", representative.getPriority());
            patch.put("moduleId", representative.getModuleId());
            patch.put("updatedAt", updatedAt);

            firestore.collection("users").document(representative.getUserId())
                    .collection("todos")
                    .whereEqualTo("recurrenceGroupId", representative.getRecurrenceGroupId())
                    .get()
                    .addOnSuccessListener(snap -> {
                        WriteBatch batch = firestore.batch();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                            batch.update(doc.getReference(), patch);
                        }
                        batch.commit()
                                .addOnFailureListener(e -> Log.e(TAG, "Firestore group sync failed", e));
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Firestore group sync query failed", e));
        } catch (Exception e) {
            Log.e(TAG, "Firestore group sync error", e);
        }
    }

    private Todo toDomain(TodoEntity e) {
        if (e == null) return null;
        Todo todo = new Todo(e.getTodoId(), e.getUserId(), e.getTitle());
        todo.setModuleId(e.getModuleId());
        todo.setDescription(e.getDescription());
        todo.setPriority(e.getPriority());
        todo.setDueDate(e.getDueDate());
        // Set completed state and preserved timestamp separately to avoid overwriting
        // the stored completedAt with the current time.
        todo.setCompleted(e.isCompleted());
        todo.setCompletedAt(e.getCompletedAt());
        todo.setRecurrencePattern(e.getRecurrencePattern());
        todo.setRecurrenceGroupId(e.getRecurrenceGroupId());
        todo.setRecurrenceEndDate(e.getRecurrenceEndDate());
        todo.setCreatedAt(e.getCreatedAt());
        todo.setUpdatedAt(e.getUpdatedAt());
        return todo;
    }

    private List<Todo> toDomainList(List<TodoEntity> entities) {
        List<Todo> list = new ArrayList<>();
        if (entities != null) {
            for (TodoEntity e : entities) list.add(toDomain(e));
        }
        return list;
    }

    private TodoEntity toEntity(Todo todo) {
        TodoEntity e = new TodoEntity();
        e.setTodoId(todo.getTodoId());
        e.setUserId(todo.getUserId());
        e.setModuleId(todo.getModuleId());
        e.setTitle(todo.getTitle());
        e.setDescription(todo.getDescription());
        e.setPriority(todo.getPriority());
        e.setDueDate(todo.getDueDate());
        e.setCompleted(todo.isCompleted());
        e.setCompletedAt(todo.getCompletedAt());
        e.setRecurrencePattern(todo.getRecurrencePattern());
        e.setRecurrenceGroupId(todo.getRecurrenceGroupId());
        e.setRecurrenceEndDate(todo.getRecurrenceEndDate());
        e.setCreatedAt(todo.getCreatedAt());
        e.setUpdatedAt(todo.getUpdatedAt());
        return e;
    }
}
