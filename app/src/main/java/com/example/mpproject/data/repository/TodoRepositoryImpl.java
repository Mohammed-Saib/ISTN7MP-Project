package com.example.mpproject.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.mpproject.data.local.AppDatabase;
import com.example.mpproject.data.local.dao.TodoDao;
import com.example.mpproject.data.local.entity.TodoEntity;
import com.example.mpproject.domain.model.Todo;
import com.example.mpproject.domain.repository.TodoRepository;

import java.util.ArrayList;
import java.util.List;

public class TodoRepositoryImpl implements TodoRepository {

    private final TodoDao todoDao;

    public TodoRepositoryImpl(TodoDao todoDao) {
        this.todoDao = todoDao;
    }

    @Override
    public void insert(Todo todo) {
        AppDatabase.databaseWriteExecutor.execute(() -> todoDao.insert(toEntity(todo)));
    }

    @Override
    public void update(Todo todo) {
        AppDatabase.databaseWriteExecutor.execute(() -> todoDao.update(toEntity(todo)));
    }

    @Override
    public void delete(Todo todo) {
        AppDatabase.databaseWriteExecutor.execute(() -> todoDao.delete(toEntity(todo)));
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
        });
    }

    @Override
    public void deleteGroup(String recurrenceGroupId, String userId) {
        AppDatabase.databaseWriteExecutor.execute(() -> todoDao.deleteByGroupId(recurrenceGroupId));
    }

    private Todo toDomain(TodoEntity e) {
        if (e == null) return null;
        Todo todo = new Todo(e.getTodoId(), e.getUserId(), e.getTitle());
        todo.setModuleId(e.getModuleId());
        todo.setDescription(e.getDescription());
        todo.setPriority(e.getPriority());
        todo.setDueDate(e.getDueDate());
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
